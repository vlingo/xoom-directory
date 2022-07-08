// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model;

import io.vlingo.xoom.actors.Actor;
import io.vlingo.xoom.cluster.model.attribute.Attribute;
import io.vlingo.xoom.cluster.model.attribute.AttributeSet;
import io.vlingo.xoom.cluster.model.attribute.AttributesProtocol;
import io.vlingo.xoom.common.Cancellable;
import io.vlingo.xoom.common.Scheduled;
import io.vlingo.xoom.directory.model.message.RegisterService;
import io.vlingo.xoom.directory.model.message.ServiceRegistered;
import io.vlingo.xoom.directory.model.message.ServiceUnregistered;
import io.vlingo.xoom.directory.model.message.UnregisterService;
import io.vlingo.xoom.wire.channel.ChannelReaderConsumer;
import io.vlingo.xoom.wire.message.RawMessage;
import io.vlingo.xoom.wire.multicast.MulticastPublisherReader;
import io.vlingo.xoom.wire.node.Address;
import io.vlingo.xoom.wire.node.AddressType;
import io.vlingo.xoom.wire.node.Name;
import io.vlingo.xoom.wire.node.Node;

import java.util.ArrayList;
import java.util.List;

public class DirectoryServiceActor extends Actor implements DirectoryService, ChannelReaderConsumer, Scheduled<IntervalType> {
  private static final String ServiceNamePrefix = "RegisteredService:";
  private static final String UnregisteredServiceNamePrefix = "UnregisteredService:";
  private static final String UnregisteredCount = "COUNT";
  
  private Cancellable cancellableMessageProcessing;
  private Cancellable cancellablePublishing;
  private AttributesProtocol attributesClient;
  private boolean isHealthyCluster;
  private final Node localNode;
  private final int maxMessageSize;
  private final Network network;
  private MulticastPublisherReader publisher;
  private final Timing timing;
  private final int unpublishedNotifications;
  
  public DirectoryServiceActor(
          final Node localNode,
          final Network network,
          final int maxMessageSize,
          final Timing timing,
          final int unpublishedNotifications) {
    this.localNode = localNode;
    this.network = network;
    this.maxMessageSize = maxMessageSize;
    this.timing = timing;
    this.unpublishedNotifications = unpublishedNotifications;
  }

  //=========================================
  // DirectoryService
  //=========================================

  @Override
  public void informHealthyCluster(boolean isHealthyCluster) {
    logger().info("DIRECTORY: Inform healthy cluster " + isHealthyCluster);
    if (this.isHealthyCluster != isHealthyCluster) {
      this.isHealthyCluster = isHealthyCluster;
      if (isHealthyCluster) {
        startProcessing();
      } else {
        stopProcessing();
      }
    }
  }

  @Override
  public void use(final AttributesProtocol client) {
    this.attributesClient = client;
  }
  
  //=========================================
  // Scheduled
  //=========================================

  @Override
  public void intervalSignal(final Scheduled<IntervalType> scheduled, final IntervalType data) {
    if (!isHealthyCluster) return;
    
    switch (data) {
    case Processing:
      publisher.processChannel();
      break;
    case Publishing:
      publisher.sendAvailability();
      publishAllServices();
      break;
    }
  }

  //=========================================
  // Startable
  //=========================================

  @Override
  public void start() {
    logger().info("DIRECTORY: Starting...");
    logger().info("DIRECTORY: Waiting to gain leadership...");
    
    super.start();
  }

  //=========================================
  // Stoppable
  //=========================================

  @Override
  public void stop() {
    logger().info("DIRECTORY: stopping on node: " + localNode);
    
    stopProcessing();
    
    if (publisher != null) {
      publisher.close();
    }
    
    super.stop();
  }

  //====================================
  // ChannelReaderConsumer
  //====================================

  @Override
  public void consume(final RawMessage message) {
    final String incoming = message.asTextMessage();
    
    final RegisterService registerService = RegisterService.from(incoming);
    if (registerService.isValid()) {
      final String attributeSetName = ServiceNamePrefix + registerService.name.value();
      for (final Address address : registerService.addresses) {
        final String fullAddress = address.full();
        attributesClient.add(attributeSetName, fullAddress, fullAddress);
      }
    } else {
      final UnregisterService unregisterService = UnregisterService.from(incoming);
      if (unregisterService.isValid()) {
        final String attributeSetName = ServiceNamePrefix + unregisterService.name.value();
        attributesClient.removeAll(attributeSetName);
        attributesClient.add(UnregisteredServiceNamePrefix + unregisterService.name.value(), UnregisteredCount, unpublishedNotifications);
      } else {
        logger().warn("DIRECTORY: RECEIVED UNKNOWN: " + incoming);
      }
    }
  }

  //====================================
  // internal implementation
  //====================================

  private Name named(final String prefix, final String serviceName) {
    return new Name(serviceName.substring(prefix.length()));
  }

  private void publishAllServices() {
    for (final AttributeSet set : attributesClient.all()) {
      if (set.name.startsWith(ServiceNamePrefix)) {
        publishService(set.name);
      } else if (set.name.startsWith(UnregisteredServiceNamePrefix)) {
        unpublishService(set.name);
      }
    }
  }

  private void publishService(final String name) {
    final List<Address> addresses = new ArrayList<>();
    for (final Attribute<?> attribute : attributesClient.allOf(name)) {
      addresses.add(Address.from(attribute.value.toString(), AddressType.MAIN));
    }
    publisher.send(RawMessage.from(0, 0, ServiceRegistered.as(named(ServiceNamePrefix, name), addresses).toString()));
  }

  private void unpublishService(final String name) {
    publisher.send(RawMessage.from(0, 0, ServiceUnregistered.as(named(UnregisteredServiceNamePrefix, name)).toString()));
    
    final Attribute<Integer> unregisteredNotificationsCount = attributesClient.attribute(name, UnregisteredCount);
    final int count = unregisteredNotificationsCount.value - 1;
    if (count - 1 <= 0) {
      attributesClient.removeAll(name);
    } else {
      attributesClient.replace(name, UnregisteredCount, count);
    }
  }

  @SuppressWarnings("unchecked")
  private void startProcessing() {
    if (publisher == null) {
      try {
        this.publisher =
                new MulticastPublisherReader(
                        "xoom-directory-service",
                        network.publisherGroup,
                        network.incomingPort,
                        maxMessageSize,
                        selfAs(ChannelReaderConsumer.class),
                        logger());
      } catch (Exception e) {
        final String message = "DIRECTORY: Failed to create multicast publisher/reader because: " + e.getMessage();
        logger().error(message, e);
        throw new IllegalStateException(message, e);
      }
    }
    
    if (cancellableMessageProcessing == null) {
      cancellableMessageProcessing =
              stage().scheduler().schedule(
                      selfAs(Scheduled.class),
                      IntervalType.Processing,
                      0,
                      timing.processingInterval);
    }
    
    if (cancellablePublishing == null) {
      cancellablePublishing =
              stage().scheduler().schedule(
                      selfAs(Scheduled.class),
                      IntervalType.Publishing,
                      0,
                      timing.publishingInterval);
    }
  }

  private void stopProcessing() {
    if (publisher != null) {
      try {
        publisher.close();
      } catch (Throwable t) {
        // ignore
      } finally {
        publisher = null;
      }
    }

    if (cancellableMessageProcessing != null) {
      try {
        cancellableMessageProcessing.cancel();
      } catch (Throwable t) {
        // ignore
      } finally {
        cancellableMessageProcessing = null;
      }
    }
    
    if (cancellablePublishing != null) {
      try {
        cancellablePublishing.cancel();
      } catch (Throwable t) {
        // ignore
      } finally {
        cancellablePublishing = null;
      }
    }
  }
}

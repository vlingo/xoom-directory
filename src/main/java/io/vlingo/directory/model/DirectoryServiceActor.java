// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.model;

import java.util.ArrayList;
import java.util.List;

import io.vlingo.actors.Actor;
import io.vlingo.actors.Cancellable;
import io.vlingo.actors.Scheduled;
import io.vlingo.cluster.model.attribute.Attribute;
import io.vlingo.cluster.model.attribute.AttributeSet;
import io.vlingo.cluster.model.attribute.AttributesProtocol;
import io.vlingo.directory.model.message.RegisterService;
import io.vlingo.directory.model.message.ServiceRegistered;
import io.vlingo.wire.channel.ChannelReaderConsumer;
import io.vlingo.wire.message.RawMessage;
import io.vlingo.wire.multicast.MulticastPublisherReader;
import io.vlingo.wire.node.Address;
import io.vlingo.wire.node.AddressType;
import io.vlingo.wire.node.Name;
import io.vlingo.wire.node.Node;

public class DirectoryServiceActor extends Actor implements DirectoryService, ChannelReaderConsumer, Scheduled {
  private static final String ServiceNamePrefix = "RegisteredService:";
  
  private enum IntervalType { Processing, Publishing }
  
  private Cancellable cancellableMessageProcessing;
  private Cancellable cancellablePublishing;
  private AttributesProtocol attributesClient;
  private boolean leader;
  private final Node localNode;
  private final MulticastPublisherReader publisher;
  private final Timing timing;
  
  public DirectoryServiceActor(
          final Node localNode,
          final Network network,
          final int maxMessageSize,
          final Timing timing)
  throws Throwable {
    this.localNode = localNode;
    this.timing = timing;
    
    this.publisher =
            new MulticastPublisherReader(
                    "vlingo-directory-service",
                    network.publisherGroup,
                    network.incomingPort,
                    maxMessageSize,
                    timing.processingTimeout,
                    selfAs(ChannelReaderConsumer.class),
                    logger());
  }

  //=========================================
  // DirectoryService
  //=========================================

  @Override
  public void assignLeadership() {
    leader = true;
    startProcessing();
  }

  @Override
  public void relinquishLeadership() {
    leader = false;
    stopProcessing();
  }


  @Override
  public void use(final AttributesProtocol client) {
    this.attributesClient = client;
  }

  
  //=========================================
  // Scheduled
  //=========================================

  @Override
  public void intervalSignal(final Scheduled scheduled, final Object data) {
    if (!leader) return;
    
    switch ((IntervalType) data) {
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
    logger().log("DIRECTORY: Starting...");
    logger().log("DIRECTORY: Waiting to gain leadership...");
    
    super.start();
  }

  //=========================================
  // Stoppable
  //=========================================

  @Override
  public void stop() {
    logger().log("DIRECTORY: stopping on node: " + localNode);
    
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
      logger().log("DIRECTORY: RECEIVED UNKNOWN: " + incoming);
    }
  }

  //====================================
  // internal implementation
  //====================================

  private Name named(final String serviceName) {
    return new Name(serviceName.substring(ServiceNamePrefix.length()));
  }

  private void publishAllServices() {
    for (final AttributeSet set : attributesClient.all()) {
      if (set.name.startsWith(ServiceNamePrefix)) {
        publishService(set.name);
      }
    }
  }

  private void publishService(final String name) {
    final List<Address> addresses = new ArrayList<>();
    for (final Attribute<?> attribute : attributesClient.allOf(name)) {
      addresses.add(Address.from(attribute.value.toString(), AddressType.MAIN));
    }
    publisher.send(RawMessage.from(0, 0, ServiceRegistered.as(named(name), addresses).toString()));
  }

  private void startProcessing() {
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
    if (cancellableMessageProcessing != null) {
      try {
        cancellableMessageProcessing.cancel();
      } finally {
        cancellableMessageProcessing = null;
      }
    }
    
    if (cancellablePublishing != null) {
      try {
        cancellablePublishing.cancel();
      } finally {
        cancellablePublishing = null;
      }
    }
  }
}

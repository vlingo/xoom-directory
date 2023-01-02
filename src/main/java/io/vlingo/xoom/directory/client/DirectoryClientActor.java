// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.client;

import io.vlingo.xoom.actors.Actor;
import io.vlingo.xoom.actors.Stoppable;
import io.vlingo.xoom.common.Cancellable;
import io.vlingo.xoom.common.Scheduled;
import io.vlingo.xoom.directory.client.ServiceRegistrationInfo.Location;
import io.vlingo.xoom.directory.model.message.RegisterService;
import io.vlingo.xoom.directory.model.message.ServiceRegistered;
import io.vlingo.xoom.directory.model.message.ServiceUnregistered;
import io.vlingo.xoom.directory.model.message.UnregisterService;
import io.vlingo.xoom.wire.channel.ChannelReaderConsumer;
import io.vlingo.xoom.wire.channel.SocketChannelWriter;
import io.vlingo.xoom.wire.message.ByteBufferAllocator;
import io.vlingo.xoom.wire.message.PublisherAvailability;
import io.vlingo.xoom.wire.message.RawMessage;
import io.vlingo.xoom.wire.multicast.Group;
import io.vlingo.xoom.wire.multicast.MulticastSubscriber;
import io.vlingo.xoom.wire.node.Name;

import java.nio.ByteBuffer;

public class DirectoryClientActor extends Actor implements DirectoryClient, ChannelReaderConsumer, Scheduled<Object>, Stoppable {
  private final ByteBuffer buffer;
  private final Cancellable cancellable;
  private PublisherAvailability directory;
  private SocketChannelWriter directoryChannel;
  private final ServiceDiscoveryInterest interest;
  private RawMessage registerService;
  private final MulticastSubscriber subscriber;
  
  @SuppressWarnings("unchecked")
  public DirectoryClientActor(
          final ServiceDiscoveryInterest interest,
          final Group directoryPublisherGroup,
          final int maxMessageSize,
          final long processingInterval,
          final int processingTimeout)
  throws Exception {
    this.interest = interest;
    this.buffer = ByteBufferAllocator.allocate(maxMessageSize);
    this.subscriber = new MulticastSubscriber(ClientName, directoryPublisherGroup, maxMessageSize, processingTimeout, logger());
    this.subscriber.openFor(selfAs(ChannelReaderConsumer.class));
    this.cancellable = stage().scheduler().schedule(selfAs(Scheduled.class), null, 0, processingInterval);
  }

  //====================================
  // DirectoryClient
  //====================================

  @Override
  public void register(final ServiceRegistrationInfo info) {
    final RegisterService converted = RegisterService.as(Name.of(info.name), Location.toAddresses(info.locations));
    this.registerService = RawMessage.from(0, 0, converted.toString());
  }

  @Override
  public void unregister(final String serviceName) {
    registerService = null;
    unregisterService(Name.of(serviceName));
  }

  //====================================
  // ChannelReaderConsumer
  //====================================

  @Override
  public void consume(final RawMessage message) {
    final String incoming = message.asTextMessage();
    final ServiceRegistered serviceRegistered = ServiceRegistered.from(incoming);
    if (serviceRegistered.isValid() && interest.interestedIn(serviceRegistered.name.value())) {
      interest.informDiscovered(new ServiceRegistrationInfo(serviceRegistered.name.value(), Location.from(serviceRegistered.addresses)));
    } else {
      final ServiceUnregistered serviceUnregistered = ServiceUnregistered.from(incoming);
      if (serviceUnregistered.isValid() && interest.interestedIn(serviceUnregistered.name.value())) {
        interest.informUnregistered(serviceUnregistered.name.value());
      } else {
        manageDirectoryChannel(incoming);
      }
    }
  }

  //====================================
  // Scheduled
  //====================================

  @Override
  public void intervalSignal(final Scheduled<Object> scheduled, final Object data) {
    subscriber.probeChannel();
    
    registerService();
  }

  //====================================
  // Stoppable
  //====================================

  @Override
  public void stop() {
    this.cancellable.cancel();
    
    super.stop();
  }

  //====================================
  // internal implementation
  //====================================

  private void manageDirectoryChannel(final String maybePublisherAvailability) {
    final PublisherAvailability publisherAvailability = PublisherAvailability.from(maybePublisherAvailability);
    if (publisherAvailability.isValid()) {
      if (!publisherAvailability.equals(directory)) {
        directory = publisherAvailability;
        if (directoryChannel != null) {
          directoryChannel.close();
        }
        directoryChannel = new SocketChannelWriter(directory.toAddress(), logger());
      }
    }
  }

  private void registerService() {
    if (directoryChannel != null && registerService != null) {
      final int expected = registerService.totalLength();
      final int actual = directoryChannel.write(registerService, buffer);
      if (actual != expected) {
        logger().warn("DIRECTORY CLIENT: Did not send full service registration message: " + registerService.asTextMessage());
      }
    }
  }

  private void unregisterService(final Name serviceName) {
    if (directoryChannel != null) {
      final UnregisterService unregister = UnregisterService.as(serviceName);
      final RawMessage unregisterServiceMessage = RawMessage.from(0, 0, unregister.toString());
      final int expected = unregisterServiceMessage.totalLength();
      final int actual = directoryChannel.write(unregisterServiceMessage, buffer);
      if (actual != expected) {
        logger().warn("DIRECTORY CLIENT: Did not send full service unregister message: " + unregisterServiceMessage.asTextMessage());
      }
    }
  }
}

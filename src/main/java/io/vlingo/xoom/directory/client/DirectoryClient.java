// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.client;

import io.vlingo.xoom.actors.ActorInstantiator;
import io.vlingo.xoom.actors.Definition;
import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.actors.Stoppable;
import io.vlingo.xoom.wire.multicast.Group;

public interface DirectoryClient extends Stoppable {
  public static final String ClientName = "xoom-directory-client";
  public static final int DefaultMaxMessageSize = 32767;
  public static final int DefaultProcessingInterval = 1000;
  public static final int DefaultProcessingTimeout = 10;

  public static DirectoryClient instance(
          final Stage stage,
          final ServiceDiscoveryInterest interest,
          final Group directoryPublisherGroup) {

    return instance(stage, interest, directoryPublisherGroup,
            DefaultMaxMessageSize, DefaultProcessingInterval, DefaultProcessingTimeout);
  }

  public static DirectoryClient instance(
          final Stage stage,
          final ServiceDiscoveryInterest interest,
          final Group directoryPublisherGroup,
          final int maxMessageSize,
          final long processingInterval,
          final int processingTimeout) {

    final Definition definition =
            Definition.has(
                    DirectoryClientActor.class,
                    new DirectoryClientInstantiator(interest, directoryPublisherGroup, maxMessageSize, processingInterval, processingTimeout),
                    ClientName);

    return stage.actorFor(DirectoryClient.class, definition);
  }

  void register(final ServiceRegistrationInfo info);
  void unregister(final String serviceName);

  static class DirectoryClientInstantiator implements ActorInstantiator<DirectoryClientActor> {
    private static final long serialVersionUID = -1557181522116028941L;

    private final ServiceDiscoveryInterest interest;
    private final Group directoryPublisherGroup;
    private final int maxMessageSize;
    private final long processingInterval;
    private final int processingTimeout;

    public DirectoryClientInstantiator(
            final ServiceDiscoveryInterest interest,
            final Group directoryPublisherGroup,
            final int maxMessageSize,
            final long processingInterval,
            final int processingTimeout) {
      this.interest = interest;
      this.directoryPublisherGroup = directoryPublisherGroup;
      this.maxMessageSize = maxMessageSize;
      this.processingInterval = processingInterval;
      this.processingTimeout = processingTimeout;
    }

    @Override
    public DirectoryClientActor instantiate() {
      try {
        return new DirectoryClientActor(interest, directoryPublisherGroup, maxMessageSize, processingInterval, processingTimeout);
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to instantiate " + type() + " because: " + e.getMessage(), e);
      }
    }

    @Override
    public Class<DirectoryClientActor> type() {
      return DirectoryClientActor.class;
    }
  }
}

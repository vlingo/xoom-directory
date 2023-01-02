// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model;

import io.vlingo.xoom.actors.ActorInstantiator;
import io.vlingo.xoom.actors.Definition;
import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.actors.Startable;
import io.vlingo.xoom.actors.Stoppable;
import io.vlingo.xoom.cluster.model.attribute.AttributesProtocol;
import io.vlingo.xoom.wire.multicast.Group;
import io.vlingo.xoom.wire.node.Node;

public interface DirectoryService extends Startable, Stoppable {

  static DirectoryService instance(
          final Stage stage,
          final Node localNode) {

    final Network network =
            new Network(
                    new Group(Properties.instance.directoryGroupAddress(), Properties.instance.directoryGroupPort()),
                    Properties.instance.directoryIncomingPort());

    final int maxMessageSize = Properties.instance.directoryMessageBufferSize();

    final Timing timing =
            new Timing(
                    Properties.instance.directoryMessageProcessingInterval(),
                    Properties.instance.directoryMessagePublishingInterval());

    final int unpublishedNotifications = Properties.instance.directoryUnregisteredServiceNotifications();

    final DirectoryService directoryService =
            DirectoryService.instance(
                    stage,
                    localNode,
                    network,
                    maxMessageSize,
                    timing,
                    unpublishedNotifications);

    return directoryService;
  }

  static DirectoryService instance(
          final Stage stage,
          final Node localNode,
          final Network network,
          final int maxMessageSize,
          final Timing timing,
          final int unpublishedNotifications) {

    final Definition definition =
            Definition.has(
                    DirectoryServiceActor.class,
                    new DirectoryServiceInstantiator(localNode, network, maxMessageSize, timing, unpublishedNotifications),
                    "xoom-directory-service");

    return stage.actorFor(DirectoryService.class, definition);
  }

  void informHealthyCluster(boolean isHealthyCluster);
  void use(final AttributesProtocol client);

  class Network {
    public final Group publisherGroup;
    public final int incomingPort;

    public Network(final Group publisherGroup, final int incomingPort) {
      this.publisherGroup = publisherGroup;
      this.incomingPort = incomingPort;
    }
  }

  class Timing {
    public final int processingInterval;
    public final int publishingInterval;

    public Timing(final int processingInterval, final int publishingInterval) {
      this.processingInterval = processingInterval;
      this.publishingInterval = publishingInterval;
    }
  }

  class DirectoryServiceInstantiator implements ActorInstantiator<DirectoryServiceActor> {
    private static final long serialVersionUID = -5865652881873161440L;

    private final Node localNode;
    private final Network network;
    private final int maxMessageSize;
    private final Timing timing;
    private final int unpublishedNotifications;

    public DirectoryServiceInstantiator(
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

    @Override
    public DirectoryServiceActor instantiate() {
      try {
        return new DirectoryServiceActor(localNode, network, maxMessageSize, timing, unpublishedNotifications);
      } catch (Throwable e) {
        throw new IllegalArgumentException("Failed to instantiate " + type() + " because: " + e.getMessage(), e);
      }
    }

    @Override
    public Class<DirectoryServiceActor> type() {
      return DirectoryServiceActor.class;
    }
  }
}

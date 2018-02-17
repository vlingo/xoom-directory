// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.model;

import io.vlingo.cluster.model.application.ClusterApplicationAdapter;
import io.vlingo.cluster.model.attribute.AttributesClient;
import io.vlingo.directory.model.DirectoryService.Network;
import io.vlingo.directory.model.DirectoryService.Timing;
import io.vlingo.wire.multicast.Group;
import io.vlingo.wire.node.Id;
import io.vlingo.wire.node.Node;

public class DirectoryApplication extends ClusterApplicationAdapter {
  private final DirectoryService directoryService;
  private boolean leading;
  private final Node localNode;
  
  public DirectoryApplication(final Node localNode) {
    this.localNode = localNode;
    
    final Network network =
            new Network(
                    new Group(Properties.instance.directoryGroupAddress(), Properties.instance.directoryGroupPort()),
                    Properties.instance.directoryIncomingPort());

    final int maxMessageSize = Properties.instance.directoryMessageBufferSize();

    final Timing timing =
            new Timing(
                    Properties.instance.directoryMessageProcessingInterval(),
                    Properties.instance.directoryMessageProcessingTimeout(),
                    Properties.instance.directoryMessagePublishingInterval());
    
    final int unpublishedNotifications = Properties.instance.directoryUnregisteredServiceNotifications();

    this.directoryService =
            DirectoryService.instance(
                    stageNamed("vlingo-directory"),
                    localNode,
                    network,
                    maxMessageSize,
                    timing,
                    unpublishedNotifications);
  }

  //====================================
  // ClusterApplication
  //====================================

  @Override
  public void informAttributesClient(final AttributesClient client) {
     logger().log("DIRECTORY: Attributes Client received.");
     
     directoryService.use(client);
  }

  @Override
  public void informLeaderElected(final Id leaderId, final boolean isHealthyCluster, final boolean isLocalNodeLeading) {
     logger().log("DIRECTORY: Leader elected: " + leaderId);
     
    if (isLocalNodeLeading) {
      leading = true;
       logger().log("DIRECTORY: Assigned leadership; starting processing.");
       directoryService.assignLeadership();
    } else {
      leading = false;
      logger().log("DIRECTORY: Remote node assigned leadership: " + leaderId);
      
      // prevent split brain in case another leader pushes in. if this node
      // is not currently leading this operation will have no harm.
      directoryService.relinquishLeadership();
    }
  }

  @Override
  public void informLeaderLost(final Id lostLeaderId, final boolean isHealthyCluster) {
     logger().log("DIRECTORY: Leader lost: " + lostLeaderId);
     
     if (localNode.id().equals(lostLeaderId)) {
       leading = false;
       directoryService.relinquishLeadership();
     }
  }

  @Override
  public void informLocalNodeShutDown(final Id nodeId) {
    logger().log("DIRECTORY: Local node left cluster: " + nodeId + "; relinquishing leadership");
    leading = false;
    
    // prevent split brain in case another leader pushes in. if this node
    // is not currently leading this operation will have no harm.
    directoryService.relinquishLeadership();
  }

  @Override
  public void informNodeLeftCluster(final Id nodeId, final boolean isHealthyCluster) {
    if (localNode.id().equals(nodeId)) {
      logger().log("DIRECTORY: Node left cluster: " + nodeId + "; relinquishing leadership");
      leading = false;
      
      // prevent split brain in case another leader pushes in. if this node
      // is not currently leading this operation will have no harm.
      directoryService.relinquishLeadership();
    } else {
      logger().log("DIRECTORY: Node left cluster: " + nodeId + (isHealthyCluster ? "; cluster still healthy" : "; cluster not healthy"));
    }
  }

  @Override
  public void informQuorumAchieved() {
    if (leading) {
      logger().log("DIRECTORY: Quorum reachieved; restarting processing.");
      directoryService.assignLeadership();
    } else {
      logger().log("DIRECTORY: Quorum achieved");
    }
  }

  @Override
  public void informQuorumLost() {
    logger().log("DIRECTORY: Quorum lost; pausing processing.");
    
    if (leading) {
      directoryService.relinquishLeadership();
    }
  }

  @Override
  public void stop() {
    directoryService.stop();
    
    super.stop();
  }
}

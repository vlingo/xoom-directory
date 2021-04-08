// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model;

import io.vlingo.xoom.cluster.model.application.ClusterApplicationAdapter;
import io.vlingo.xoom.cluster.model.attribute.AttributesProtocol;
import io.vlingo.xoom.wire.node.Id;
import io.vlingo.xoom.wire.node.Node;

public class DirectoryApplication extends ClusterApplicationAdapter {
  private final DirectoryService directoryService;
  private boolean leading;
  private final Node localNode;
  
  public DirectoryApplication(final Node localNode) {
    this.localNode = localNode;

    this.directoryService = DirectoryService.instance(stage(), localNode);
  }

  //====================================
  // ClusterApplication
  //====================================

  @Override
  public void informAttributesClient(final AttributesProtocol client) {
     logger().debug("DIRECTORY: Attributes Client received.");
     
     directoryService.use(client);
  }

  @Override
  public void informLeaderElected(final Id leaderId, final boolean isHealthyCluster, final boolean isLocalNodeLeading) {
     logger().info("DIRECTORY: Leader elected: " + leaderId);
     
    if (isLocalNodeLeading) {
      leading = true;
       logger().debug("DIRECTORY: Assigned leadership; starting processing.");
       directoryService.assignLeadership();
    } else {
      leading = false;
      logger().debug("DIRECTORY: Remote node assigned leadership: " + leaderId);
      
      // prevent split brain in case another leader pushes in. if this node
      // is not currently leading this operation will have no harm.
      directoryService.relinquishLeadership();
    }
  }

  @Override
  public void informLeaderLost(final Id lostLeaderId, final boolean isHealthyCluster) {
     logger().warn("DIRECTORY: Leader lost: " + lostLeaderId);
     
     if (localNode.id().equals(lostLeaderId)) {
       leading = false;
       directoryService.relinquishLeadership();
     }
  }

  @Override
  public void informLocalNodeShutDown(final Id nodeId) {
    logger().info("DIRECTORY: Local node left cluster: " + nodeId + "; relinquishing leadership");
    leading = false;
    
    // prevent split brain in case another leader pushes in. if this node
    // is not currently leading this operation will have no harm.
    directoryService.relinquishLeadership();
  }

  @Override
  public void informNodeLeftCluster(final Id nodeId, final boolean isHealthyCluster) {
    if (localNode.id().equals(nodeId)) {
      logger().info("DIRECTORY: Node left cluster: " + nodeId + "; relinquishing leadership");
      leading = false;
      
      // prevent split brain in case another leader pushes in. if this node
      // is not currently leading this operation will have no harm.
      directoryService.relinquishLeadership();
    } else {
      logger().info("DIRECTORY: Node left cluster: " + nodeId + (isHealthyCluster ? "; cluster still healthy" : "; cluster not healthy"));
    }
  }

  @Override
  public void informQuorumAchieved() {
    if (leading) {
      logger().debug("DIRECTORY: Quorum reachieved; restarting processing.");
      directoryService.assignLeadership();
    } else {
      logger().info("DIRECTORY: Quorum achieved");
    }
  }

  @Override
  public void informQuorumLost() {
    logger().warn("DIRECTORY: Quorum lost; pausing processing.");
    
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

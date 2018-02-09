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
    
    this.directoryService = DirectoryService.instance(stage(), localNode, network, maxMessageSize, timing);
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
       logger().log("DIRECTORY: Assigned leadership; starting processing.");
       
       directoryService.assignLeadership();
    }
  }

  @Override
  public void informLeaderLost(final Id lostLeaderId, final boolean isHealthyCluster) {
     logger().log("DIRECTORY: Leader lost: " + lostLeaderId);
     
     if (localNode.id().equals(lostLeaderId)) {
       directoryService.relinquishLeadership();
     }
  }

  @Override
  public void stop() {
    directoryService.stop();
    
    super.stop();
  }
}

// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
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

import java.util.Collection;
import java.util.stream.Collectors;

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
  public void informAllLiveNodes(Collection<Node> liveNodes, boolean isHealthyCluster) {
    String nodeNames = liveNodes.stream()
            .map(node -> node.name().value())
            .collect(Collectors.joining(", "));
    logger().info("DIRECTORY: All live nodes: " + nodeNames + " and the cluster is healthy: " + isHealthyCluster);
    directoryService.informHealthyCluster(isHealthyCluster);
  }

  @Override
  public void informNodeJoinedCluster(Id nodeId, boolean isHealthyCluster) {
    logger().info("DIRECTORY: Node " + nodeId + " joined the cluster and the cluster is healthy: " + isHealthyCluster);
    directoryService.informHealthyCluster(isHealthyCluster);
  }

  @Override
  public void informNodeLeftCluster(final Id nodeId, final boolean isHealthyCluster) {
    logger().info("DIRECTORY: Node left cluster: " + nodeId + (isHealthyCluster ? "; cluster still healthy" : "; cluster not healthy"));
    directoryService.informHealthyCluster(isHealthyCluster);
  }

  @Override
  public void informClusterIsHealthy(boolean isHealthyCluster) {
    logger().info("DIRECTORY: The cluster is healthy: " + isHealthyCluster);
    directoryService.informHealthyCluster(isHealthyCluster);
  }

  @Override
  public void stop() {
    directoryService.stop();
    
    super.stop();
  }
}

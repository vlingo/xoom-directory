// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vlingo.actors.Definition;
import io.vlingo.actors.testkit.TestActor;
import io.vlingo.actors.testkit.TestWorld;
import io.vlingo.directory.client.DirectoryClient;
import io.vlingo.directory.client.DirectoryClientActor;
import io.vlingo.directory.client.MockServiceDiscoveryInterest;
import io.vlingo.directory.client.ServiceRegistrationInfo;
import io.vlingo.directory.client.ServiceRegistrationInfo.Location;
import io.vlingo.directory.model.DirectoryService.Network;
import io.vlingo.directory.model.DirectoryService.Timing;
import io.vlingo.wire.multicast.Group;
import io.vlingo.wire.node.Host;
import io.vlingo.wire.node.Id;
import io.vlingo.wire.node.Name;
import io.vlingo.wire.node.Node;

public class DirectoryServiceTest {
  private TestActor<DirectoryClient> client1;
  private TestActor<DirectoryClient> client2;
  private TestActor<DirectoryClient> client3;
  private TestActor<DirectoryService> directory;
  private Group group;
  private MockServiceDiscoveryInterest interest1;
  private MockServiceDiscoveryInterest interest2;
  private MockServiceDiscoveryInterest interest3;
  private Node node;
  private TestWorld testWorld;
  
  @Test
  public void testRegisterDiscoverOne() {
    directory.actor().start();
    directory.actor().use(new TestAttributesClient());
    directory.actor().assignLeadership();
    
    final Location location = new Location("test-host", 1234);
    final ServiceRegistrationInfo info = new ServiceRegistrationInfo("test-service", Arrays.asList(location));
    client1.actor().register(info);
    
    pause();
    
    assertFalse(interest1.servicesSeen.isEmpty());
    assertTrue(interest1.servicesSeen.contains("test-service"));
    assertFalse(interest1.discoveredServices.isEmpty());
    assertTrue(interest1.discoveredServices.contains(info));
  }
  
  @Test
  public void testRegisterDiscoverMutiple() {
    directory.actor().start();
    directory.actor().use(new TestAttributesClient());
    directory.actor().assignLeadership();
    
    final Location location1 = new Location("test-host1", 1234);
    final ServiceRegistrationInfo info1 = new ServiceRegistrationInfo("test-service1", Arrays.asList(location1));
    client1.actor().register(info1);
    
    final Location location2 = new Location("test-host2", 1234);
    final ServiceRegistrationInfo info2 = new ServiceRegistrationInfo("test-service2", Arrays.asList(location2));
    client2.actor().register(info2);
    
    final Location location3 = new Location("test-host3", 1234);
    final ServiceRegistrationInfo info3 = new ServiceRegistrationInfo("test-service3", Arrays.asList(location3));
    client3.actor().register(info3);
    
    pause();
    
    for (final MockServiceDiscoveryInterest interest : Arrays.asList(interest1, interest2, interest3)) {
      assertFalse(interest.servicesSeen.isEmpty());
      assertTrue(interest.servicesSeen.contains("test-service1"));
      assertTrue(interest.servicesSeen.contains("test-service2"));
      assertTrue(interest.servicesSeen.contains("test-service3"));
      assertFalse(interest.discoveredServices.isEmpty());
      assertTrue(interest.discoveredServices.contains(info1));
      assertTrue(interest.discoveredServices.contains(info2));
      assertTrue(interest.discoveredServices.contains(info3));
    }
  }
  
  @Before
  public void setUp() {
    testWorld = TestWorld.start("test");
    
    node = Node.with(Id.of(1), Name.of("node1"), Host.of("localhost"), 37371, 37372);
    
    group = new Group("237.37.37.1", 37371);
    
    directory = testWorld.actorFor(
            Definition.has(
                    DirectoryServiceActor.class,
                    Definition.parameters(node, new Network(group, 37399), 1024, new Timing(100, 100, 100))),
            DirectoryService.class);
    
    interest1 = new MockServiceDiscoveryInterest();
    
    client1 = testWorld.actorFor(
            Definition.has(
                    DirectoryClientActor.class,
                    Definition.parameters(interest1, group, 1024, 50, 10)),
            DirectoryClient.class);
    
    interest2 = new MockServiceDiscoveryInterest();
    
    client2 = testWorld.actorFor(
            Definition.has(
                    DirectoryClientActor.class,
                    Definition.parameters(interest2, group, 1024, 50, 10)),
            DirectoryClient.class);
    
    interest3 = new MockServiceDiscoveryInterest();
    
    client3 = testWorld.actorFor(
            Definition.has(
                    DirectoryClientActor.class,
                    Definition.parameters(interest3, group, 1024, 50, 10)),
            DirectoryClient.class);
  }
  
  @After
  public void tearDown() {
    directory.actor().stop();
    client1.actor().stop();
    client2.actor().stop();
    client3.actor().stop();
    testWorld.terminate();
  }
  
  private void pause() {
    try { Thread.sleep(1000); } catch (Exception e) { }
  }
}

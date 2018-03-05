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
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vlingo.actors.Definition;
import io.vlingo.actors.testkit.TestActor;
import io.vlingo.actors.testkit.TestUntil;
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
  private List<MockServiceDiscoveryInterest> interests;
  private Node node;
  private TestWorld testWorld;
  
  @Test
  public void testShouldInformInterest() {
    directory.actor().start();
    directory.actor().use(new TestAttributesClient());
    
    // directory assigned leadership
    directory.actor().assignLeadership();
    
    final Location location = new Location("test-host", 1234);
    final ServiceRegistrationInfo info = new ServiceRegistrationInfo("test-service", Arrays.asList(location));
    
    MockServiceDiscoveryInterest.interestsSeen = TestUntil.happenings(6);
    client1.actor().register(info);
    MockServiceDiscoveryInterest.interestsSeen.completes();
    
    assertFalse(interest1.servicesSeen.isEmpty());
    assertTrue(interest1.servicesSeen.contains("test-service"));
    assertFalse(interest1.discoveredServices.isEmpty());
    assertTrue(interest1.discoveredServices.contains(info));
  }
  
  @Test
  public void testShouldUnregister() {
    directory.actor().start();
    directory.actor().use(new TestAttributesClient());
    
    // directory assigned leadership
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
    
    client1.actor().unregister(info1.name);
    pause();
    
    for (final MockServiceDiscoveryInterest interest : Arrays.asList(interest2, interest3)) {
      System.out.println("COUNT: " + (interest.servicesSeen.size() + interest.discoveredServices.size() + interest.unregisteredServices.size()));
      assertFalse(interest.servicesSeen.isEmpty());
      assertTrue(interest.servicesSeen.contains(info1.name));
      assertFalse(interest.discoveredServices.isEmpty());
      assertTrue(interest.discoveredServices.contains(info1));
      assertFalse(interest.unregisteredServices.isEmpty());
      System.out.print(interest.unregisteredServices.toString());
      assertTrue(interest.unregisteredServices.contains(info1.name));
    }
  }

  @Test
  public void testShouldNotInformInterest() {
    directory.actor().start();
    directory.actor().use(new TestAttributesClient());
    
    // directory NOT assigned leadership
    directory.actor().relinquishLeadership(); // actually never had leadership, but be explicit and prove no harm
    
    final Location location1 = new Location("test-host1", 1234);
    final ServiceRegistrationInfo info1 = new ServiceRegistrationInfo("test-service1", Arrays.asList(location1));
    client1.actor().register(info1);
    
    pause();
    
    assertTrue(interest1.servicesSeen.isEmpty());
    assertFalse(interest1.servicesSeen.contains("test-service"));
    assertTrue(interest1.discoveredServices.isEmpty());
    assertFalse(interest1.discoveredServices.contains(info1));
  }

  @Test
  public void testAlteredLeadership() {
    directory.actor().start();
    directory.actor().use(new TestAttributesClient());
    
    // START directory assigned leadership
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
    
    for (final MockServiceDiscoveryInterest interest : interests) {
      assertFalse(interest.servicesSeen.isEmpty());
      assertTrue(interest.servicesSeen.contains("test-service1"));
      assertTrue(interest.servicesSeen.contains("test-service2"));
      assertTrue(interest.servicesSeen.contains("test-service3"));
      assertFalse(interest.discoveredServices.isEmpty());
      assertTrue(interest.discoveredServices.contains(info1));
      assertTrue(interest.discoveredServices.contains(info2));
      assertTrue(interest.discoveredServices.contains(info3));
    }
    
    // ALTER directory relinquished leadership
    directory.actor().relinquishLeadership();
    pause();
    for (final MockServiceDiscoveryInterest interest : interests) {
      interest.servicesSeen.clear();
      interest.discoveredServices.clear();
    }
    
    pause();
    
    for (final MockServiceDiscoveryInterest interest : interests) {
      assertTrue(interest.servicesSeen.isEmpty());
      assertFalse(interest.servicesSeen.contains("test-service1"));
      assertFalse(interest.servicesSeen.contains("test-service2"));
      assertFalse(interest.servicesSeen.contains("test-service3"));
      assertTrue(interest.discoveredServices.isEmpty());
      assertFalse(interest.discoveredServices.contains(info1));
      assertFalse(interest.discoveredServices.contains(info2));
      assertFalse(interest.discoveredServices.contains(info3));
    }
    
    // ALTER directory assigned leadership
    directory.actor().assignLeadership();
    pause();
    for (final MockServiceDiscoveryInterest interest : interests) {
      interest.servicesSeen.clear();
      interest.discoveredServices.clear();
    }
    
    pause();
    
    for (final MockServiceDiscoveryInterest interest : interests) {
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
    
    for (final MockServiceDiscoveryInterest interest : interests) {
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
                    Definition.parameters(node, new Network(group, 37399), 1024, new Timing(100, 100, 100), 20)),
            DirectoryService.class);
    
    interest1 = new MockServiceDiscoveryInterest("interest1");
    
    client1 = testWorld.actorFor(
            Definition.has(
                    DirectoryClientActor.class,
                    Definition.parameters(interest1, group, 1024, 50, 10)),
            DirectoryClient.class);
    
    interest2 = new MockServiceDiscoveryInterest("interest2");
    
    client2 = testWorld.actorFor(
            Definition.has(
                    DirectoryClientActor.class,
                    Definition.parameters(interest2, group, 1024, 50, 10)),
            DirectoryClient.class);
    
    interest3 = new MockServiceDiscoveryInterest("interest3");
    
    client3 = testWorld.actorFor(
            Definition.has(
                    DirectoryClientActor.class,
                    Definition.parameters(interest3, group, 1024, 50, 10)),
            DirectoryClient.class);
    
    interests = Arrays.asList(interest1, interest2, interest3);
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
    pause(1000);
  }
  
  private void pause(final long milliseconds) {
    try { Thread.sleep(milliseconds); } catch (Exception e) { }
  }
}

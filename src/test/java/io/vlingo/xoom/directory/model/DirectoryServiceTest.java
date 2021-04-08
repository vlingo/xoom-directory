// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.vlingo.xoom.actors.Definition;
import io.vlingo.xoom.actors.testkit.TestActor;
import io.vlingo.xoom.actors.testkit.TestWorld;
import io.vlingo.xoom.directory.client.DirectoryClient;
import io.vlingo.xoom.directory.client.DirectoryClient.DirectoryClientInstantiator;
import io.vlingo.xoom.directory.client.DirectoryClientActor;
import io.vlingo.xoom.directory.client.MockServiceDiscoveryInterest;
import io.vlingo.xoom.directory.client.ServiceRegistrationInfo;
import io.vlingo.xoom.directory.client.ServiceRegistrationInfo.Location;
import io.vlingo.xoom.directory.model.DirectoryService.DirectoryServiceInstantiator;
import io.vlingo.xoom.directory.model.DirectoryService.Network;
import io.vlingo.xoom.directory.model.DirectoryService.Timing;
import io.vlingo.xoom.wire.multicast.Group;
import io.vlingo.xoom.wire.node.Host;
import io.vlingo.xoom.wire.node.Id;
import io.vlingo.xoom.wire.node.Name;
import io.vlingo.xoom.wire.node.Node;

public class DirectoryServiceTest {
    private TestActor<DirectoryService> directory;
    private Group group;
    private TestWorld testWorld;

    @Test
    @Ignore
    public void testShouldInformInterest() {
        directory.actor().start();
        directory.actor().use(new TestAttributesClient());

        // directory assigned leadership
        directory.actor().assignLeadership();

        final ServiceRegistrationInfo info = getServiceRegistrationInfo("test-host", "test-service");

        final MockServiceDiscoveryInterest interest = new MockServiceDiscoveryInterest("interest1", 2);
        final TestActor<DirectoryClient> client = getClient(interest);

        try {
            client.actor().register(info);

            assertFalse(interest.getServicesSeen().isEmpty());
            assertTrue(interest.getServicesSeen().contains(info.name));
            assertFalse(interest.getDiscoveredServices().isEmpty());
            assertTrue(interest.getDiscoveredServices().contains(info));
        } finally {
            stopClient(client);
        }
    }

    @Test
    public void testShouldNotInformInterest() {
        directory.actor().start();
        directory.actor().use(new TestAttributesClient());

        // directory NOT assigned leadership
        directory.actor().relinquishLeadership(); // actually never had leadership, but be explicit and prove no harm

        final ServiceRegistrationInfo registrationInfo = getServiceRegistrationInfo("test-host", "test-service");
        final MockServiceDiscoveryInterest interest = new MockServiceDiscoveryInterest("interest1", 0);
        final TestActor<DirectoryClient> client = getClient(interest);

        try {
            client.actor().register(registrationInfo);

            pause();

            assertTrue(interest.getServicesSeen().isEmpty());
            assertFalse(interest.getServicesSeen().contains(registrationInfo.name));
            assertTrue(interest.getDiscoveredServices().isEmpty());
            assertFalse(interest.getDiscoveredServices().contains(registrationInfo));
        } finally {
            stopClient(client);
        }
    }


    @Test
    @Ignore
    public void testShouldUnregister() {
        directory.actor().start();
        directory.actor().use(new TestAttributesClient());

        // directory assigned leadership
        directory.actor().assignLeadership();

        int nrClients = 3;

        List<TestActor<DirectoryClient>> clients = new ArrayList<>(nrClients);
        List<MockServiceDiscoveryInterest> interests = new ArrayList<>(nrClients);

        for (int i = 0; i < nrClients; i++) {
            final MockServiceDiscoveryInterest interest1 = new MockServiceDiscoveryInterest("interest" + i, 2 * nrClients);
            final TestActor<DirectoryClient> client1 = getClient(interest1);
            clients.add(client1);
            interests.add(interest1);
        }

        List<ServiceRegistrationInfo> registrationInfos = new ArrayList<>(nrClients);

        for (int i = 0; i < nrClients; i++) {
            final ServiceRegistrationInfo info = getServiceRegistrationInfo("test-host" + i, "test-service" + i);
            registrationInfos.add(info);
            clients.get(i).actor().register(info);
        }

        try {
            assertRegistered(interests, registrationInfos);

            //Unregister first service
            final ServiceRegistrationInfo unregisteredInfo = registrationInfos.get(0);
            clients.get(0).actor().unregister(unregisteredInfo.name);
            pause();

            for (final MockServiceDiscoveryInterest interest : interests.subList(1, nrClients)) {
                assertEquals(3, interest.getServicesSeen().size());
                assertFalse(interest.getServicesSeen().isEmpty());
                assertTrue(interest.getServicesSeen().contains(unregisteredInfo.name));
                assertEquals(3, interest.getDiscoveredServices().size());
                assertFalse(interest.getDiscoveredServices().isEmpty());
                assertTrue(interest.getDiscoveredServices().contains(unregisteredInfo));
                assertEquals(1, interest.getUnregisteredServices().size());
                assertFalse(interest.getUnregisteredServices().isEmpty());
                assertTrue(interest.getUnregisteredServices().contains(unregisteredInfo.name));
            }
        } finally {
            clients.forEach(this::stopClient);
        }
    }


    @Test
    @Ignore
    public void testAlteredLeadership() {
        directory.actor().start();
        directory.actor().use(new TestAttributesClient());

        // START directory assigned leadership
        directory.actor().assignLeadership();

        int nrClients = 3;

        List<TestActor<DirectoryClient>> clients = new ArrayList<>(nrClients);
        List<MockServiceDiscoveryInterest> interests = new ArrayList<>(nrClients);

        for (int i = 0; i < nrClients; i++) {
            final MockServiceDiscoveryInterest interest1 = new MockServiceDiscoveryInterest("interest" + i, 2 * nrClients);
            final TestActor<DirectoryClient> client1 = getClient(interest1);
            clients.add(client1);
            interests.add(interest1);
        }

        List<ServiceRegistrationInfo> registrationInfos = new ArrayList<>(nrClients);
        try {
            for (int i = 0; i < nrClients; i++) {
                final ServiceRegistrationInfo info = getServiceRegistrationInfo("test-host" + i, "test-service" + i);
                registrationInfos.add(info);
                clients.get(i).actor().register(info);
            }

            pause();

            assertRegistered(interests, registrationInfos);

            // ALTER directory relinquished leadership
            directory.actor().relinquishLeadership();
            pause();
            for (final MockServiceDiscoveryInterest interest : interests) {
                interest.getServicesSeen().clear();
                interest.getDiscoveredServices().clear();
            }

            pause();

            assertNotRegistered(interests, registrationInfos);

            // ALTER directory assigned leadership
            directory.actor().assignLeadership();
            pause();
            for (final MockServiceDiscoveryInterest interest : interests) {
                interest.getServicesSeen().clear();
                interest.getDiscoveredServices().clear();
            }

            pause();

            assertRegistered(interests, registrationInfos);
        } finally {
            clients.forEach(this::stopClient);
        }
    }


    @Test
    @Ignore
    public void testRegisterDiscoverMultiple() {
        directory.actor().start();
        directory.actor().use(new TestAttributesClient());
        directory.actor().assignLeadership();

        int nrClients = 3;

        List<TestActor<DirectoryClient>> clients = new ArrayList<>(nrClients);
        List<MockServiceDiscoveryInterest> interests = new ArrayList<>(nrClients);

        for (int i = 0; i < nrClients; i++) {
            final MockServiceDiscoveryInterest interest1 = new MockServiceDiscoveryInterest("interest" + i, 2 * nrClients);
            final TestActor<DirectoryClient> client1 = getClient(interest1);
            clients.add(client1);
            interests.add(interest1);
        }

        List<ServiceRegistrationInfo> registrationInfos = new ArrayList<>(nrClients);

        for (int i = 0; i < nrClients; i++) {
            final ServiceRegistrationInfo info = getServiceRegistrationInfo("test-host" + i, "test-service" + i);
            registrationInfos.add(info);
            clients.get(i).actor().register(info);
        }

        try {
            assertRegistered(interests, registrationInfos);
        } finally {
            clients.forEach(this::stopClient);
        }
    }


    @Before
    public void setUp() {
        testWorld = TestWorld.start("test");

        Node node = Node.with(Id.of(1), Name.of("node1"), Host.of("localhost"), 37371, 37372);

        group = new Group("237.37.37.1", 37371);

        directory = testWorld.actorFor(
                DirectoryService.class,
                Definition.has(
                        DirectoryServiceActor.class,
                        new DirectoryServiceInstantiator(node, new Network(group, 37399), 1024, new Timing(100, 100), 20)));

    }

    @After
    public void tearDown() {
        directory.actor().stop();
        testWorld.terminate();
    }


    private TestActor<DirectoryClient> getClient(MockServiceDiscoveryInterest interest) {
        return testWorld.actorFor(
                DirectoryClient.class,
                Definition.has(
                        DirectoryClientActor.class,
                        new DirectoryClientInstantiator(interest, group, 1024, 50, 10)));
    }

    private ServiceRegistrationInfo getServiceRegistrationInfo(String s, String s2) {
        final Location location = new Location(s, 1234);
        return new ServiceRegistrationInfo(s2, Collections.singletonList(location));
    }

    private void assertRegistered(List<MockServiceDiscoveryInterest> interests, List<ServiceRegistrationInfo> registrationInfos) {
        for (final MockServiceDiscoveryInterest interest : interests) {
            assertFalse(interest.getServicesSeen().isEmpty());
            assertFalse(interest.getDiscoveredServices().isEmpty());

            registrationInfos.forEach(info -> {
                assertTrue(interest.getServicesSeen().contains(info.name));
                assertTrue(interest.getDiscoveredServices().contains(info));
            });
        }
    }

    private void assertNotRegistered(List<MockServiceDiscoveryInterest> interests, List<ServiceRegistrationInfo> registrationInfos) {
        for (final MockServiceDiscoveryInterest interest : interests) {
            assertTrue(interest.getServicesSeen().isEmpty());
            assertTrue(interest.getDiscoveredServices().isEmpty());

            registrationInfos.forEach(info -> {
                assertFalse(interest.getServicesSeen().contains(info.name));
                assertFalse(interest.getDiscoveredServices().contains(info));
            });
        }
    }

    private void stopClient(TestActor<DirectoryClient> client) {
        client.actor().stop();
    }

    private void pause() {
        pause(1000);
    }

    private void pause(final long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
        }
    }
}

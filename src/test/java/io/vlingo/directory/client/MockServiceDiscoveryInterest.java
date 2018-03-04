package io.vlingo.directory.client;

import java.util.ArrayList;
import java.util.List;

import io.vlingo.actors.testkit.TestUntil;

public class MockServiceDiscoveryInterest implements ServiceDiscoveryInterest {
  public static TestUntil interestsSeen;
  
  public List<ServiceRegistrationInfo> discoveredServices;
  public List<String> servicesSeen;
  public List<String> unregisteredServices;
  
  public MockServiceDiscoveryInterest() {
    discoveredServices = new ArrayList<>();
    servicesSeen = new ArrayList<>();
    unregisteredServices = new ArrayList<>();
  }
  
  @Override
  public boolean interestedIn(final String serviceName) {
    if (!servicesSeen.contains(serviceName)) {
      servicesSeen.add(serviceName);
      if (interestsSeen != null) interestsSeen.happened();
    }
    return true;
  }

  @Override
  public void informDiscovered(final ServiceRegistrationInfo discoveredService) {
    if (!discoveredServices.contains(discoveredService)) {
      discoveredServices.add(discoveredService);
      if (interestsSeen != null) interestsSeen.happened();
    }
  }

  @Override
  public void informUnregistered(final String unregisteredServiceName) {
    if (!unregisteredServices.contains(unregisteredServiceName)) {
      System.out.println("informUnregistered: " + unregisteredServiceName);
      unregisteredServices.add(unregisteredServiceName);
      if (interestsSeen != null) interestsSeen.happened();
    }
  }
}

package io.vlingo.directory.client;

import java.util.ArrayList;
import java.util.List;

public class MockServiceDiscoveryInterest implements ServiceDiscoveryInterest {
  public List<ServiceRegistrationInfo> discoveredServices;
  public List<String> servicesSeen;
  
  public MockServiceDiscoveryInterest() {
    discoveredServices = new ArrayList<>();
    servicesSeen = new ArrayList<>();
  }
  
  @Override
  public boolean interestedIn(final String serviceName) {
    if (!servicesSeen.contains(serviceName)) {
      servicesSeen.add(serviceName);
    }
    return true;
  }

  @Override
  public void inform(final ServiceRegistrationInfo discoveredService) {
    if (!discoveredServices.contains(discoveredService)) {
      discoveredServices.add(discoveredService);
    }
  }
}

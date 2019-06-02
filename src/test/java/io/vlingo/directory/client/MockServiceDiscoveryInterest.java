package io.vlingo.directory.client;

import io.vlingo.actors.testkit.AccessSafely;

import java.util.ArrayList;
import java.util.List;

public class MockServiceDiscoveryInterest implements ServiceDiscoveryInterest {

  private final AccessSafely result;
  public String name;
  private List<ServiceRegistrationInfo> discoveredServices;
  private List<String> servicesSeen;
  private List<String> unregisteredServices;

  public MockServiceDiscoveryInterest(final String name, int times) {
    this.name = name;
    discoveredServices = new ArrayList<>();
    servicesSeen = new ArrayList<>();
    unregisteredServices = new ArrayList<>();

    result = AccessSafely.afterCompleting(times);
    result.writingWith("discoveredServices", (final ServiceRegistrationInfo discoveredService) -> {
      if (!this.discoveredServices.contains(discoveredService)) {
        this.discoveredServices.add(discoveredService);
      }
    });
    result.readingWith("discoveredServices", () -> discoveredServices);

    result.writingWith("servicesSeen", (String serviceName) -> {
      if (!this.servicesSeen.contains(serviceName)) {
        this.servicesSeen.add(serviceName);
      }
    });
    result.readingWith("servicesSeen", () -> servicesSeen);

    result.writingWith("unregisteredServices", (String unregisteredServiceName) -> {
      if (!this.unregisteredServices.contains(unregisteredServiceName)) {
        this.unregisteredServices.add(unregisteredServiceName);
      }
    });
    result.readingWith("unregisteredServices", () -> unregisteredServices);
  }

  @Override
  public boolean interestedIn(final String serviceName) {
    if (!this.servicesSeen.contains(serviceName)) {
      this.result.writeUsing("servicesSeen", serviceName);
    }
    return true;
  }

  @Override
  public void informDiscovered(final ServiceRegistrationInfo discoveredService) {
    if (!this.discoveredServices.contains(discoveredService)) {
      this.result.writeUsing("discoveredServices", discoveredService);
    }
  }

  @Override
  public void informUnregistered(final String unregisteredServiceName) {
    if (!this.unregisteredServices.contains(unregisteredServiceName)) {
      this.result.writeUsing("unregisteredServices", unregisteredServiceName);
    }
  }

  public List<ServiceRegistrationInfo> getDiscoveredServices() {
    return result.readFrom("discoveredServices");
  }

  public List<String> getServicesSeen() {
    return result.readFrom("servicesSeen");
  }

  public List<String> getUnregisteredServices() {
    return result.readFrom("unregisteredServices");
  }
}

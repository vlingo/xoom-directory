// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.vlingo.xoom.wire.node.Address;
import io.vlingo.xoom.wire.node.AddressType;
import io.vlingo.xoom.wire.node.Host;

public final class ServiceRegistrationInfo {
  public final String name;
  public final Collection<Location> locations;
  
  public ServiceRegistrationInfo(final String name, final Collection<Location> locations) {
    this.name = name;
    this.locations = locations;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == null || other.getClass() != ServiceRegistrationInfo.class) {
      return false;
    }
    
    final ServiceRegistrationInfo otherInfo = (ServiceRegistrationInfo) other;
    
    return this.name.equals(otherInfo.name) && this.locations.equals(otherInfo.locations);
  }

  @Override
  public String toString() {
    return "ServiceRegistrationInfo[name=" + name + ", locations=" + locations + "]";
  }

  public static class Location {
    static Location from(final Address address) {
      return new Location(address.hostName(), address.port());
    }
    
    static Collection<Location> from(final Collection<Address> addresses) {
      final List<Location> locations = new ArrayList<>(addresses.size());
      for (final Address address : addresses) {
        locations.add(new Location(address.hostName(), address.port()));
      }
      return locations;
    }
    
    static Collection<Address> toAddresses(final Collection<Location> locations) {
      final List<Address> addresses = new ArrayList<>(locations.size());
      for (final Location location : locations) {
        addresses.add(new Address(Host.of(location.address), location.port, AddressType.MAIN));
      }
      return addresses;
    }

    public final String address;
    public final int port;
    
    public Location(final String address, final int port) {
      this.address = address;
      this.port = port;
    }

    @Override
    public boolean equals(final Object other) {
      if (other == null || other.getClass() != Location.class) {
        return false;
      }
      
      final Location otherLocation = (Location) other;
      
      return this.address.equals(otherLocation.address) && this.port == otherLocation.port;
    }

    @Override
    public String toString() {
      return "Location[address=" + address + ", port=" + port + "]";
    }
  }
}

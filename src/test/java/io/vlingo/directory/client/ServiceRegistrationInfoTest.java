package io.vlingo.directory.client;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import io.vlingo.directory.client.ServiceRegistrationInfo.Location;
import io.vlingo.wire.node.Address;
import io.vlingo.wire.node.AddressType;
import io.vlingo.wire.node.Host;

public class ServiceRegistrationInfoTest {

  @Test
  public void testInfo() {
    final ServiceRegistrationInfo info =
            new ServiceRegistrationInfo(
                    "test-service",
                    Arrays.asList(new Location("1.2.3.4", 111), new Location("1.2.3.45", 222)));
    
    assertEquals("test-service", info.name);
    assertEquals(2, info.locations.size());
    final Iterator<Location> iter = info.locations.iterator();
    assertEquals(new Location("1.2.3.4", 111), iter.next());
    assertEquals(new Location("1.2.3.45", 222), iter.next());
    
    final ServiceRegistrationInfo infoAgain =
            new ServiceRegistrationInfo(
                    "test-service",
                    Arrays.asList(new Location("1.2.3.4", 111), new Location("1.2.3.45", 222)));
    
    assertEquals(info, infoAgain);
  }
  
  @Test
  public void testToFrom() {
    final Collection<Location> twoLocations = Arrays.asList(new Location("1.2.3.4", 111), new Location("1.2.3.45", 222));
    final Collection<Address> twoAddresses = Location.toAddresses(twoLocations);
    
    final Iterator<Address> iter = twoAddresses.iterator();
    assertEquals(new Address(Host.of("1.2.3.4"), 111, AddressType.MAIN), iter.next());
    assertEquals(new Address(Host.of("1.2.3.45"), 222, AddressType.MAIN), iter.next());
    
    assertEquals(twoLocations.iterator().next(), Location.from(twoAddresses.iterator().next()));
    
    final Collection<Location> convertedLocations = Location.from(twoAddresses);
    assertEquals(twoLocations, convertedLocations);
  }
}

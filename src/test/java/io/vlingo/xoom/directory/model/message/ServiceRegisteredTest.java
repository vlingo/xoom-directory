// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import io.vlingo.xoom.wire.node.Address;
import io.vlingo.xoom.wire.node.AddressType;
import io.vlingo.xoom.wire.node.Host;
import io.vlingo.xoom.wire.node.Name;

public class ServiceRegisteredTest {
  private final String textMessage = "SRVCREGD\nnm=test-service\naddr=1.2.3.4:111\naddr=1.2.3.45:222";

  @Test
  public void testMessage() {
    final ServiceRegistered serviceRegistered =
            new ServiceRegistered(
                    Name.of("test-service"),
                    Arrays.asList(
                            Address.from(Host.of("1.2.3.4"), 111, AddressType.MAIN),
                            Address.from(Host.of("1.2.3.45"), 222, AddressType.MAIN)));
    
    assertEquals(2, serviceRegistered.addresses.size());
    assertEquals(textMessage, serviceRegistered.toString());
  }
  
  @Test
  public void testValidity() {
    final ServiceRegistered serviceRegistered =
            new ServiceRegistered(
                    Name.of("test-service"),
                    Arrays.asList(
                            Address.from(Host.of("1.2.3.4"), 111, AddressType.MAIN),
                            Address.from(Host.of("1.2.3.45"), 222, AddressType.MAIN)));
    
    assertTrue(serviceRegistered.isValid());
    assertFalse(ServiceRegistered.from("blah").isValid());
    assertTrue(ServiceRegistered.from(textMessage).isValid());
  }
}

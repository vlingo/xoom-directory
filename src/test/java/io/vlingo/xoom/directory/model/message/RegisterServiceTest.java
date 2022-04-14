// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.vlingo.xoom.wire.node.Address;
import io.vlingo.xoom.wire.node.AddressType;
import io.vlingo.xoom.wire.node.Host;
import io.vlingo.xoom.wire.node.Name;

public class RegisterServiceTest {
  private final String textMessage = "REGSRVC\nnm=test-service\naddr=1.2.3.4:111";

  @Test
  public void testMessage() {
    final RegisterService registerService =
            new RegisterService(
                    Name.of("test-service"),
                    Address.from(Host.of("1.2.3.4"), 111, AddressType.MAIN));
    
    assertEquals(1, registerService.addresses.size());
    assertEquals(textMessage, registerService.toString());
  }
  
  @Test
  public void testValidity() {
    final RegisterService registerService =
            new RegisterService(
                    Name.of("test-service"),
                    Address.from(Host.of("1.2.3.4"), 111, AddressType.MAIN));
    
    assertTrue(registerService.isValid());
    assertFalse(RegisterService.from("blah").isValid());
    assertTrue(RegisterService.from(textMessage).isValid());
  }
}

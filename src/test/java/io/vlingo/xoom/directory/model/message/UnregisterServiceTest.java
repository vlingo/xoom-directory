// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
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

import io.vlingo.xoom.wire.node.Name;

public class UnregisterServiceTest {
  private final String textMessage = "UNREGSRVC\nnm=test-service";

  @Test
  public void testMessage() {
    final UnregisterService unregisterService = UnregisterService.as(Name.of("test-service"));
    
    assertEquals(Name.of("test-service"), unregisterService.name);
    assertEquals(textMessage, unregisterService.toString());
  }

  @Test
  public void testValidity() {
    final UnregisterService unregisterService = UnregisterService.as(Name.of("test-service"));
    
    assertTrue(unregisterService.isValid());
    assertFalse(UnregisterService.from("blah").isValid());
    assertTrue(UnregisterService.from(textMessage).isValid());
  }
}

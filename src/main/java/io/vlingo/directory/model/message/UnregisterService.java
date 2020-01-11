// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.model.message;

import io.vlingo.wire.message.Message;
import io.vlingo.wire.message.MessagePartsBuilder;
import io.vlingo.wire.node.Name;

public class UnregisterService implements Message {
  public static final String TypeName = "UNREGSRVC";
  
  public final Name name;
  
  public static UnregisterService from(final String content) {
    if (content.startsWith(TypeName)) {
      final Name name = MessagePartsBuilder.nameFrom(content);
      return new UnregisterService(name);
    }
    return new UnregisterService(Name.NO_NODE_NAME);
  }
  
  public static UnregisterService as(final Name name) {
    return new UnregisterService(name);
  }
  
  public UnregisterService(final Name name) {
    this.name = name;
  }
  
  public boolean isValid() {
    return !name.hasNoName();
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append(TypeName).append("\n").append("nm=").append(name.value());
    
    return builder.toString();
  }
}

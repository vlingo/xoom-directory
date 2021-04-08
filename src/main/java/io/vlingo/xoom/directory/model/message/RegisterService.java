// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model.message;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.vlingo.xoom.wire.message.Message;
import io.vlingo.xoom.wire.message.MessagePartsBuilder;
import io.vlingo.xoom.wire.node.Address;
import io.vlingo.xoom.wire.node.AddressType;
import io.vlingo.xoom.wire.node.Name;

public class RegisterService implements Message {
  public static final String TypeName = "REGSRVC";
  
  public final Set<Address> addresses;
  public final Name name;
  
  public static RegisterService from(final String content) {
    if (content.startsWith(TypeName)) {
      final Name name = MessagePartsBuilder.nameFrom(content);
      final AddressType type = AddressType.MAIN;
      final Set<Address> addresses = MessagePartsBuilder.addressesFromRecord(content, type);
      return new RegisterService(name, addresses);
    }
    return new RegisterService(Name.NO_NODE_NAME);
  }
  
  public static RegisterService as(final Name name, final Address address) {
    return new RegisterService(name, address);
  }
  
  public static RegisterService as(final Name name, final Collection<Address> addresses) {
    return new RegisterService(name, addresses);
  }
  
  public RegisterService(final Name name, final Address address) {
    this(name);
    this.addresses.add(address);
  }
  
  public RegisterService(final Name name, final Collection<Address> addresses) {
    this(name);
    this.addresses.addAll(addresses);
  }
  
  public boolean isValid() {
    return !name.hasNoName();
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    final AddressType type = AddressType.MAIN;

    builder.append(TypeName).append("\n").append("nm=").append(name.value());
    
    for (final Address address : addresses) {
      builder.append("\n").append(type.field()).append(address.host().name()).append(":").append(address.port());
    }
    
    return builder.toString();
  }
  
  private RegisterService(final Name name) {
    this.name = name;
    this.addresses = new HashSet<>();
  }
}

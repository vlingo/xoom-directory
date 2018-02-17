// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.client;

import io.vlingo.actors.Definition;
import io.vlingo.actors.Stage;
import io.vlingo.actors.Stoppable;
import io.vlingo.wire.multicast.Group;

public interface DirectoryClient extends Stoppable {
  public static final String ClientName = "vlingo-directory-client";
  public static final int DefaultMaxMessageSize = 32767;
  public static final int DefaultProcessingInterval = 1000;
  public static final int DefaultProcessingTimeout = 10;

  public static DirectoryClient instance(
          final Stage stage,
          final ServiceDiscoveryInterest interest,
          final Group directoryPublisherGroup) {
    
    return instance(stage, interest, directoryPublisherGroup,
            DefaultMaxMessageSize, DefaultProcessingInterval, DefaultProcessingTimeout);
  }

  public static DirectoryClient instance(
          final Stage stage,
          final ServiceDiscoveryInterest interest,
          final Group directoryPublisherGroup,
          final int maxMessageSize,
          final long processingInterval,
          final int processingTimeout) {
    
    final Definition definition =
            Definition.has(
                    DirectoryClientActor.class,
                    Definition.parameters(interest, directoryPublisherGroup, maxMessageSize, processingInterval, processingTimeout),
                    ClientName);
    
    return stage.actorFor(definition, DirectoryClient.class);
  }
  
  void register(final ServiceRegistrationInfo info);
  void unregister(final String serviceName);
}

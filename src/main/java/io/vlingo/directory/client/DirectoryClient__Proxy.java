// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.client;

import java.util.function.Consumer;

import io.vlingo.actors.Actor;
import io.vlingo.actors.DeadLetter;
import io.vlingo.actors.LocalMessage;
import io.vlingo.actors.Mailbox;
import io.vlingo.actors.Stoppable;

public class DirectoryClient__Proxy implements DirectoryClient {
  private static final String representationConclude0 = "conclude()";

  private final Actor actor;
  private final Mailbox mailbox;

  public DirectoryClient__Proxy(final Actor actor, final Mailbox mailbox) {
    this.actor = actor;
    this.mailbox = mailbox;
  }

  @Override
  public void conclude() {
    if (!actor.isStopped()) {
      final Consumer<Stoppable> consumer = (actor) -> actor.conclude();
      if (mailbox.isPreallocated()) { mailbox.send(actor, Stoppable.class, consumer, null, representationConclude0); }
      else { mailbox.send(new LocalMessage<Stoppable>(actor, Stoppable.class, consumer, representationConclude0)); }
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, representationConclude0));
    }
  }

  @Override
  public boolean isStopped() {
    return actor.isStopped();
  }

  @Override
  public void stop() {
    if (!actor.isStopped()) {
      final Consumer<Stoppable> consumer = (actor) -> actor.stop();
      mailbox.send(new LocalMessage<Stoppable>(actor, Stoppable.class, consumer, "stop()"));
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, "stop()"));
    }
  }

  @Override
  public void register(final ServiceRegistrationInfo info) {
    if (!actor.isStopped()) {
      final Consumer<DirectoryClient> consumer = (actor) -> actor.register(info);
      mailbox.send(new LocalMessage<DirectoryClient>(actor, DirectoryClient.class, consumer, "register(ServiceRegistrationInfo)"));
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, "register(ServiceRegistrationInfo)"));
    }
  }

  @Override
  public void unregister(final String serviceName) {
    if (!actor.isStopped()) {
      final Consumer<DirectoryClient> consumer = (actor) -> actor.unregister(serviceName);
      mailbox.send(new LocalMessage<DirectoryClient>(actor, DirectoryClient.class, consumer, "unregister(String)"));
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, "unregister(String)"));
    }
  }
}

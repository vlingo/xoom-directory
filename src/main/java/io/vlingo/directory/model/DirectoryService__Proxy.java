// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.model;

import java.util.function.Consumer;

import io.vlingo.actors.Actor;
import io.vlingo.actors.DeadLetter;
import io.vlingo.actors.LocalMessage;
import io.vlingo.actors.Mailbox;
import io.vlingo.actors.Startable;
import io.vlingo.actors.Stoppable;
import io.vlingo.cluster.model.attribute.AttributesProtocol;

public class DirectoryService__Proxy implements DirectoryService {
  private static final String representationConclude0 = "conclude()";

  private final Actor actor;
  private final Mailbox mailbox;

  public DirectoryService__Proxy(final Actor actor, final Mailbox mailbox) {
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
  public void start() {
    if (!actor.isStopped()) {
      final Consumer<Startable> consumer = (actor) -> actor.start();
      mailbox.send(new LocalMessage<Startable>(actor, Startable.class, consumer, "start()"));
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, "start()"));
    }
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
  public void assignLeadership() {
    if (!actor.isStopped()) {
      final Consumer<DirectoryService> consumer = (actor) -> actor.assignLeadership();
      mailbox.send(new LocalMessage<DirectoryService>(actor, DirectoryService.class, consumer, "assignLeadership()"));
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, "assignLeadership()"));
    }
  }

  @Override
  public void relinquishLeadership() {
    if (!actor.isStopped()) {
      final Consumer<DirectoryService> consumer = (actor) -> actor.relinquishLeadership();
      mailbox.send(new LocalMessage<DirectoryService>(actor, DirectoryService.class, consumer, "relinquishLeadership()"));
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, "relinquishLeadership()"));
    }
  }

  @Override
  public void use(final AttributesProtocol client) {
    if (!actor.isStopped()) {
      final Consumer<DirectoryService> consumer = (actor) -> actor.use(client);
      mailbox.send(new LocalMessage<DirectoryService>(actor, DirectoryService.class, consumer, "use(AttributesProtocol)"));
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, "use(AttributesProtocol)"));
    }
  }
}

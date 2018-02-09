// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.directory.model;

import java.util.function.Consumer;

import io.vlingo.actors.Actor;
import io.vlingo.actors.LocalMessage;
import io.vlingo.actors.Mailbox;
import io.vlingo.actors.Startable;
import io.vlingo.actors.Stoppable;
import io.vlingo.cluster.model.attribute.AttributesProtocol;

public class DirectoryService__Proxy implements DirectoryService {
  private final Actor actor;
  private final Mailbox mailbox;

  public DirectoryService__Proxy(final Actor actor, final Mailbox mailbox) {
    this.actor = actor;
    this.mailbox = mailbox;
  }

  @Override
  public boolean isStopped() {
    return actor.isStopped();
  }

  @Override
  public void start() {
    final Consumer<Startable> consumer = (actor) -> actor.start();
    mailbox.send(new LocalMessage<Startable>(actor, Startable.class, consumer, "start()"));
  }

  @Override
  public void stop() {
    final Consumer<Stoppable> consumer = (actor) -> actor.stop();
    mailbox.send(new LocalMessage<Stoppable>(actor, Stoppable.class, consumer, "stop()"));
  }

  @Override
  public void assignLeadership() {
    final Consumer<DirectoryService> consumer = (actor) -> actor.assignLeadership();
    mailbox.send(new LocalMessage<DirectoryService>(actor, DirectoryService.class, consumer, "assignLeadership()"));
  }

  @Override
  public void relinquishLeadership() {
    final Consumer<DirectoryService> consumer = (actor) -> actor.relinquishLeadership();
    mailbox.send(new LocalMessage<DirectoryService>(actor, DirectoryService.class, consumer, "relinquishLeadership()"));
  }

  @Override
  public void use(final AttributesProtocol client) {
    final Consumer<DirectoryService> consumer = (actor) -> actor.use(client);
    mailbox.send(new LocalMessage<DirectoryService>(actor, DirectoryService.class, consumer, "use(AttributesProtocol)"));
  }
}

/*
 * Copyright 2012-2013 Eligotech BV.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eligosource.eventsourced.core

import akka.actor._

/**
 * Allows actors with a stackable [[org.eligosource.eventsourced.core.Receiver]],
 * [[org.eligosource.eventsourced.core.Emitter]] and/or
 * [[org.eligosource.eventsourced.core.Eventsourced]] modification to change their behavior
 * with `become()` and `unbecome()` without loosing the functionality implemented by these
 * traits.
 *
 * On the other hand, actors that use `context.become()` to change their behavior will loose
 * their `Receiver`, `Emitter` and/or `Eventsourced` functionality.
 */
trait Behavior extends Actor {
  private val emptyBehaviorStack: List[Receive] = Nil
  private var behaviorStack: List[Receive] = super.receive :: emptyBehaviorStack

  abstract override def receive: Receive = {
    case msg => invoke(msg)
  }

  final def invoke(msg: Any) {
    val behavior = behaviorStack.head
    if (behavior.isDefinedAt(msg)) behavior(msg) else unhandled(msg)
  }

  /**
   * Puts `behavior` on the hotswap stack. This will only affect the behavior of the actor
   * that has been modified with this stackable trait.
   *
   * @param behavior new behavior
   * @param discardOld if `true`, an `unbecome()` will be issued prior to pushing `behavior`.
   */
  def become(behavior: Actor.Receive, discardOld: Boolean = true) {
    behaviorStack = behavior :: (if (discardOld && behaviorStack.nonEmpty) behaviorStack.tail else behaviorStack)
  }

  /**
   * Reverts the behavior to the previous one on the hotswap stack. This will only affect the
   * behavior of the actor that has been modified with this stackable trait.
   */
  def unbecome() {
    behaviorStack = if (behaviorStack.isEmpty || behaviorStack.tail.isEmpty) super.receive :: emptyBehaviorStack else behaviorStack.tail
  }
}

package pl.zuchos.example

import akka.actor.Actor
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, ZeroRequestStrategy}

class LazyDataSubscriber() extends ActorSubscriber {

  override def receive: Actor.Receive = {
    case OnNext(s) =>
  }

  override val requestStrategy = ZeroRequestStrategy
}
package pl.zuchos.example

import akka.actor.Actor
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import pl.zuchos.example.DataPublisher.Publish

import scala.collection.mutable

class DataPublisher extends ActorPublisher[Data] {

  var queue: mutable.Queue[Data] = mutable.Queue()

  override def receive: Actor.Receive = {
    case Publish(s) => queue.enqueue(s)
      publishIfNeeded()
    case Request(cnt) =>
      publishIfNeeded()
    case Cancel => context.stop(self)
    case _ =>
  }

  def publishIfNeeded() = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      onNext(queue.dequeue())
    }
  }
}

object DataPublisher {

  case class Publish(data: Data)

}

case class Data(sender: String, body: String)
package pl.zuchos.example.actors

import akka.actor.Actor
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import pl.zuchos.example.actors.DataPublisher.Publish

import scala.collection.mutable
import scala.util.{Failure, Success}

class DataPublisher(val bufferSize: Int) extends ActorPublisher[Data] {

  if (bufferSize <= 0) throw new IllegalArgumentException("Buffer should be positive number...")

  var queue: mutable.Queue[Data] = mutable.Queue()

  override def receive: Actor.Receive = {
    case Publish(s) =>
      cacheIfPossible(s)
    case Request(cnt) =>
      publishIfNeeded()
    case Cancel => context.stop(self)
    case _ =>
  }

  private def cacheIfPossible(s: Data) {
    if (queue.length == bufferSize) {
      sender() ! Failure(new BufferOverflow)
    } else {
      queue.enqueue(s)
      sender() ! Success()
      publishIfNeeded()
    }
  }

  def publishIfNeeded() = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      onNext(queue.dequeue())
    }
  }
}

class BufferOverflow extends Exception

object DataPublisher {

  case class Publish(data: Data)

}

case class Data(sender: String, body: String)
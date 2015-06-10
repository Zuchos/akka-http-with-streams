package pl.zuchos.example

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{Accepted, InternalServerError, ServiceUnavailable}
import akka.stream.FlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import pl.zuchos.example.actors.{BufferOverflow, DataPublisher}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

trait PublisherService[D] {

  // Using the same names as in RouteTest we are making life easier
  // (no need to override them in test classes)
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: FlowMaterializer

  implicit val timeout: Timeout

  def dataProcessingDefinition: Sink[D, Unit]

  def publisherBufferSize: Int

  def dataPublisherRef: ActorRef = dataPublisherReference

  private var dataPublisherReference: ActorRef = _

  def run() = {
    dataPublisherReference = system.actorOf(Props[DataPublisher[D]](new DataPublisher[D](publisherBufferSize)))
    val dataPublisher = ActorPublisher[D](dataPublisherRef)
    Source(dataPublisher).runWith(dataProcessingDefinition)
  }
}

object PublisherService {
  def respond(publisherResponse: Future[Any])(implicit executionContextExecutor: ExecutionContextExecutor) = publisherResponse.map {
    case Success(_) => HttpResponse(Accepted, entity = "Data received")
    case Failure(_: BufferOverflow) => HttpResponse(ServiceUnavailable, entity = "Try again later...")
    case _ => HttpResponse(InternalServerError, entity = "Something gone terribly wrong...")
  }
}


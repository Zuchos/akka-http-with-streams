package pl.zuchos.example

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{Accepted, InternalServerError, ServiceUnavailable}
import akka.http.scaladsl.server.Route
import akka.stream.FlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import pl.zuchos.example.actors.{BufferOverflow, DataPublisher}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

trait RestService[D] {

  // Using the same names as in RouteTest we are making life easier
  // (no need to override them in test classes)
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: FlowMaterializer

  implicit val timeout = Timeout(1 seconds)

  def dataProcessingDefinition: Sink[D, Unit]

  def publisherBufferSize: Int

  def routes: Route

  val dataPublisherRef = system.actorOf(Props[DataPublisher[D]](new DataPublisher[D](publisherBufferSize)))
  val dataPublisher = ActorPublisher[D](dataPublisherRef)

  Source(dataPublisher).runWith(dataProcessingDefinition)

  def respond(publisherResponse: Future[Any]) = publisherResponse.map {
    case Success(_) => HttpResponse(Accepted, entity = "Data received")
    case Failure(_: BufferOverflow) => HttpResponse(ServiceUnavailable, entity = "Try again later...")
    case _ => HttpResponse(InternalServerError, entity = "Something gone terribly wrong...")
  }

}

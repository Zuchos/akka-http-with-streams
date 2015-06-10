package pl.zuchos.example

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{Accepted, InternalServerError, ServiceUnavailable}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import pl.zuchos.example.actors.DataPublisher.Publish
import pl.zuchos.example.actors.{BufferOverflow, Data, DataPublisher}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/**
 * Simple 'Hello world!' service
 */
trait SimpleService {

  // Using the same names as in RouteTest we are making life easier
  // (no need to override them in test classes)
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: FlowMaterializer

  implicit val timeout = Timeout(1 seconds)

  def dataProcessingDefinition: Sink[Data, Unit]

  def publisherBufferSize: Int

  val dataPublisherRef = system.actorOf(Props[DataPublisher](new DataPublisher(publisherBufferSize)))
  val dataPublisher = ActorPublisher[Data](dataPublisherRef)

  Source(dataPublisher).runWith(dataProcessingDefinition)

  val routes = {
    path("hello") {
      get {
        complete("Hello World!")
      }
    } ~
      path("data") {
        (post & entity(as[String]) & parameter('sender)) {
          (dataAsString, sender: String) =>
            complete {
              val publisherResponse: Future[Any] = dataPublisherRef ? Publish(Data(sender, dataAsString))
              publisherResponse.map {
                case Success(_) => HttpResponse(Accepted, entity = "Data received")
                case Failure(_: BufferOverflow) => HttpResponse(ServiceUnavailable, entity = "Try again later...")
                case _ =>
                  HttpResponse(InternalServerError, entity = "Something gone terribly wrong...")
              }
            }
        }
      }
  }
}

/**
 * Server that runs our service
 */
object NaiveServer extends App with SimpleService {

  override implicit lazy val system = ActorSystem()
  override implicit lazy val executor = system.dispatcher
  override implicit lazy val materializer = ActorFlowMaterializer()

  override def dataProcessingDefinition: Sink[Data, Unit] = Flow[Data].map(d => {
    println(d)
    d
  }).to(Sink.ignore)

  override def publisherBufferSize: Int = 1000

  val config = ConfigFactory.load()
  Http().bindAndHandle(routes, config.getString("http.host"), config.getInt("http.port"))

}



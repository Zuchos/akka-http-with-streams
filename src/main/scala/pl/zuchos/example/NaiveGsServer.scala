package pl.zuchos.example

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import pl.zuchos.example.DataPublisher.Publish

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
  implicit val timeout = Timeout(5 seconds)


  val dataPublisherRef = system.actorOf(Props[DataPublisher](new DataPublisher(1000)))
  val dataPublisher = ActorPublisher[Data](dataPublisherRef)

  Source(dataPublisher)
    .runForeach(
      (x: Data) =>
        println(s"Data from ${x.sender} are being processed: ${x.body}")
    )
    .onComplete(_ => system.shutdown())


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
                case Success(_) => HttpResponse(StatusCodes.OK, entity = "Data received")
                case Failure(_: BufferOverflow) => HttpResponse(StatusCodes.ServiceUnavailable, entity = "Try again later...")
                case _ =>
                  HttpResponse(StatusCodes.InternalServerError, entity = "Something gone terribly wrong...")
              }
            }
        }
      }
  }
}

/**
 * Server that runs our service
 */
object NaiveGsServer extends App with SimpleService {

  override implicit val system = ActorSystem()

  override implicit val executor = system.dispatcher

  override implicit val materializer = ActorFlowMaterializer()

  val config = ConfigFactory.load()

  Http().bindAndHandle(routes, config.getString("http.host"), config.getInt("http.port"))

}



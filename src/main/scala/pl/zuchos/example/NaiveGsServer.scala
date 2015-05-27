package pl.zuchos.example

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor

/**
 * Simple 'Hello world!' service
 */
trait SimpleService {

  // Using the same names as in RouteTest we are making life easier
  // (no need to override them in test classes)
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: FlowMaterializer

  val dataPublisherRef = system.actorOf(Props[DataPublisher])
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
      (post & entity(as[String]) & parameter('sender.as[String])) {
        (dataAsString, sender: String) =>
          complete {
            dataPublisherRef ! Data(sender, dataAsString)
            HttpResponse(StatusCodes.OK, entity = "Data received")
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



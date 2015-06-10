package pl.zuchos.example.usage

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import pl.zuchos.example.PublisherService
import pl.zuchos.example.PublisherService.respond
import pl.zuchos.example.actors.DataPublisher.Publish

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait DataService {

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: FlowMaterializer

  implicit val timeout: Timeout = Timeout(1 seconds)

  def dataPublisherRef: ActorRef

  def routes: Route = {
    path("data") {
      (post & entity(as[String]) & parameter('sender)) {
        (dataAsString, sender: String) =>
          complete {
            respond(dataPublisherRef ? Publish(Data(sender, dataAsString)))
          }
      }
    }
  }
}

case class Data(sender: String, body: String)

/**
 * Server that runs our service
 */
object NaiveServer extends App with DataService with PublisherService[Data] {

  override implicit lazy val system = ActorSystem()
  override implicit lazy val executor = system.dispatcher
  override implicit lazy val materializer = ActorFlowMaterializer()

  override def publisherBufferSize: Int = 1000

  override def dataProcessingDefinition: Sink[Data, Unit] = Flow[Data].map(d => {
    println(d)
    d
  }).to(Sink.ignore)

  run()
  val config = ConfigFactory.load()

  import RouteResult._

  Http().bindAndHandle(routes, config.getString("http.host"), config.getInt("http.port"))

}



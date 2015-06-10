package pl.zuchos.example.usage

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.typesafe.config.ConfigFactory
import pl.zuchos.example.RestService
import pl.zuchos.example.actors.DataPublisher.Publish

trait DataService extends RestService[Data] {

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
object NaiveServer extends App with DataService {

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



package pl.zuchos.example

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes.{Accepted, OK, ServiceUnavailable}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.{FlowMaterializer, OperationAttributes}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import pl.zuchos.example.actors.Data

import scala.concurrent.ExecutionContextExecutor

class SimpleServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach {

  val that = this
  var simpleService: SimpleService = _

  override protected def beforeEach(): Unit = {
    simpleService = new SimpleService {

      override def publisherBufferSize: Int = 2

      override implicit def executor: ExecutionContextExecutor = that.executor
      override implicit lazy val materializer: FlowMaterializer = that.materializer
      override implicit lazy val system: ActorSystem = that.system

      lazy val dataSubscriberRef = system.actorOf(Props[LazyDataSubscriber](new LazyDataSubscriber()))
      lazy val dataSubscriber = ActorSubscriber[Data](dataSubscriberRef)

      override def dataProcessingDefinition: Sink[Data, Unit] = Flow[Data].map(d => {
        println(s"Processing data from ${d.sender} body: ${d.body}")
        d
      }).to(Sink(dataSubscriber)).withAttributes(OperationAttributes.inputBuffer(1, 1))
    }
  }

  "SimpleService" should {
    "respond with 'Hello World!'" in {
      Get("/hello") ~> simpleService.routes ~> check {
        status shouldBe OK
        contentType shouldBe `text/plain(UTF-8)`
        responseAs[String] shouldBe "Hello World!"
      }
    }
    "respond with Data received" in {
      Post("/data?sender=Lukasz", "Test1") ~> simpleService.routes ~> check {
        status shouldBe Accepted
        val entity: String = entityAs[String]
        entity shouldBe "Data received"
      }
    }
    "respond with Data received until service will become unavailable" in {
      Post("/data?sender=Lukasz", "Test1") ~> simpleService.routes ~> check {
        status shouldBe Accepted
        val entity: String = entityAs[String]
        entity shouldBe "Data received"
      }
      Post("/data?sender=Lukasz", "Test2") ~> simpleService.routes ~> check {
        status shouldBe Accepted
        val entity: String = entityAs[String]
        entity shouldBe "Data received"
      }
      Post("/data?sender=Lukasz", "Test3") ~> simpleService.routes ~> check {
        status shouldBe Accepted
        val entity: String = entityAs[String]
        entity shouldBe "Data received"
      }
      Post("/data?sender=Lukasz", "Test4") ~> simpleService.routes ~> check {
        status shouldBe ServiceUnavailable
        val entity: String = entityAs[String]
        entity shouldBe "Try again later..."
      }
    }
  }
}


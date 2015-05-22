package pl.zuchos.example

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class SimpleServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with SimpleService {

  "SimpleService" should {
    "respond with 'Hello World!'" in {
      Get("/hello") ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `text/plain(UTF-8)`
        responseAs[String] shouldBe "Hello World!"
      }
    }
  }

}

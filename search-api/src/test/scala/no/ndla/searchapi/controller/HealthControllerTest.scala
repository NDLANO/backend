/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import com.sksamuel.elastic4s.cluster.ClusterHealthRequest
import com.sksamuel.elastic4s.http.{Handler, RequestSuccess}
import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import scala.util.{Failure, Success, Try}

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new HealthController
  addServlet(controller, "/")

  test("That /health returns 200 ok") {
    val x = Try(RequestSuccess(200, None, Map.empty, ""))
    when(
      e4sClient.execute(any[ClusterHealthRequest])(any[Handler[ClusterHealthRequest, String]], any[Manifest[String]]))
      .thenReturn(x)
    get("/") {
      status should equal(200)
    }
  }

  test("That /health returns 500 when elasticsearch not reachable") {
    when(e4sClient.execute(any[Any])(any[Handler[Any, Any]], any[Manifest[Any]]))
      .thenReturn(Failure(new java.net.ConnectException("Is no work")))
    get("/") {
      status should equal(500)
    }
  }

}

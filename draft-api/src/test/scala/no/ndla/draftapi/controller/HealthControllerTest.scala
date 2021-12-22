/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new HealthController
  addServlet(controller, "/")

  test("That /health returns 200 ok") {
    get("/") {
      status should equal(200)
    }
  }

}

/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new HealthController
  addServlet(controller, props.HealthControllerMountPoint)

  test("That /health returns 200 ok") {
    get("/health") {
      status should equal(200)
    }
  }

}

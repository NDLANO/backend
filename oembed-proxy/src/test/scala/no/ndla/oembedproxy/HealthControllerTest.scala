/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import org.scalatra.test.scalatest.ScalatraFunSuite

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new HealthController
  addServlet(controller, OEmbedProxyProperties.HealthControllerMountPoint)

  test("That /health returns 200 ok") {
    get("/health") {
      status should equal(200)
    }
  }

}

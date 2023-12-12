/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.myndla.model.api.Stats
import org.json4s.DefaultFormats
import org.scalatra.test.scalatest.ScalatraFunSuite

class StatsControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  implicit val swagger: LearningpathSwagger = new LearningpathSwagger

  lazy val controller = new StatsController()
  addServlet(controller, "/*")

  override def beforeEach(): Unit = {
    resetMocks()
  }

  test("That getting stats returns in fact stats") {
    when(folderReadService.getStats).thenReturn(Some(Stats(1, 2, 3, 4, 5, 6, List.empty)))
    get("/") {
      status should be(200)
    }
  }

}

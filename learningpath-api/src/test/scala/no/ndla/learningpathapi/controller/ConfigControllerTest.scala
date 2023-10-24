/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.TestData._
import no.ndla.learningpathapi.model.api.config.{ConfigMeta, ConfigMetaValue}
import no.ndla.learningpathapi.model.domain.config.ConfigKey
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser
import org.json4s.Formats
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success

class ConfigControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: Formats             = org.json4s.DefaultFormats
  implicit val swagger: LearningpathSwagger = new LearningpathSwagger

  lazy val controller = new ConfigController
  addServlet(controller, "/*")

  override def beforeEach(): Unit = {
    resetMocks()
    when(languageValidator.validate(any[String], any[String], any[Boolean]))
      .thenReturn(None)
  }

  test("That updating config returns 200 if all is good") {
    when(updateService.updateConfig(any[ConfigKey], any[ConfigMetaValue], any[TokenUser]))
      .thenReturn(
        Success(
          ConfigMeta(
            ConfigKey.LearningpathWriteRestricted.entryName,
            Left(true),
            NDLADate.now(),
            "someoneCool"
          )
        )
      )

    post(
      s"/${ConfigKey.LearningpathWriteRestricted.entryName}",
      body = "{\"value\": true}",
      headers = Map("Authorization" -> s"Bearer $adminScopeClientToken")
    ) {
      status should be(200)
    }

    post(
      s"/${ConfigKey.LearningpathWriteRestricted.entryName}",
      body = "{\"value\": true}",
      headers = Map("Authorization" -> s"Bearer $adminAndWriteScopeClientToken")
    ) {
      status should be(200)
    }
  }
}

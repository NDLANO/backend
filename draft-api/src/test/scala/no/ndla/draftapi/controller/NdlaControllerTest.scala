/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.draftapi.model.api.UpdatedArticle
import org.scalatra.swagger.SwaggerEngine
import org.json4s.DefaultFormats

class NdlaControllerTest extends UnitSuite with TestEnvironment {

  val ndlaController = new NdlaController {
    override protected implicit def swagger: SwaggerEngine = ???

    override protected def applicationDescription: String = ???
  }

  test("That extraction of request body parses relatedContent correctly") {
    implicit val formats = DefaultFormats.withLong
    val updatedArticle   = ndlaController.tryExtract[UpdatedArticle]("""{"revision":2,"relatedContent":[1]}""").get

    updatedArticle should be(TestData.blankUpdatedArticle.copy(revision = 2, relatedContent = Some(Seq(Right(1L)))))

    val Right(relatedId) = updatedArticle.relatedContent.get.head
    relatedId.toLong should be(1L)
  }

}

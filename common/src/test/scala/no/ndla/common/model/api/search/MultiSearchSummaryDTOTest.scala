/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.testbase.UnitTestSuiteBase

class MultiSearchSummaryDTOTest extends UnitTestSuiteBase {
  test("serialization") {
    val now         = NDLADate.now().withNano(0)
    val toSerialize = MultiSearchSummaryDTO(
      id = 1L,
      title = TitleWithHtmlDTO("Test Title", "<b>Test Title</b>", "en"),
      metaDescription = MetaDescriptionDTO("A meta description", "en"),
      metaImage = None,
      url = "https://example.com/resource/1",
      context = Some(
        ApiTaxonomyContextDTO(
          publicId = "ctx1",
          root = "rootNode",
          rootId = "root1",
          relevance = "high",
          relevanceId = "rel1",
          path = "/root/path",
          breadcrumbs = List("root", "path"),
          contextId = "ctxid1",
          contextType = "type1",
          resourceTypes = List(),
          language = "en",
          isPrimary = true,
          isActive = true,
          url = "https://example.com/context/1",
        )
      ),
      contexts = List(),
      supportedLanguages = Seq("en"),
      learningResourceType = LearningResourceType.Article,
      status = None,
      traits = List(ArticleTrait.Video),
      score = 0.99f,
      highlights = List(),
      paths = List(),
      lastUpdated = now,
      license = None,
      revisions = Seq(),
      responsible = None,
      comments = None,
      priority = None,
      resourceTypeName = None,
      parentTopicName = None,
      primaryRootName = None,
      published = None,
      favorited = None,
      resultType = SearchType.Articles,
    )
    import io.circe.syntax.*

    val serialized   = toSerialize.asJson.noSpaces
    val deserialized = CirceUtil.unsafeParse(serialized)
    deserialized.hcursor.get[String]("typename") should be(Right("MultiSearchSummaryDTO"))
    val deserializedBack = deserialized.as[MultiSearchSummaryDTO].toTry.get
    deserializedBack should be(toSerialize)
  }
}

/*
 * Part of NDLA search-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import io.circe.syntax.*
import no.ndla.common.model.domain.Title
import no.ndla.common.model.domain.learningpath.{
  EmbedType,
  EmbedUrl,
  LearningStep,
  StepStatus,
  StepType,
  Description as LPDescription
}
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.util.Success

class LearningPathIndexServiceTest extends ElasticsearchIntegrationSuite with UnitSuite with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))

  override val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    learningPathIndexService.deleteIndexAndAlias()
    learningPathIndexService.createIndexWithGeneratedName
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(myndlaApiClient.getStatsFor(any, any)).thenReturn(Success(List.empty))
  }

  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  test("That mapping contains every field after serialization") {
    val domainLearningPath = TestData.learningPath1.copy(
      learningsteps = Some(
        List(
          LearningStep(
            id = Some(1L),
            revision = Some(1),
            externalId = Some("hei"),
            learningPathId = Some(1L),
            seqNo = 1,
            title = Seq(Title("hei", "nb")),
            introduction = Seq(),
            description = Seq(LPDescription("hei", "nb")),
            embedUrl = Seq(EmbedUrl("hei", "nb", EmbedType.OEmbed)),
            `type` = StepType.TEXT,
            license = Some("hei"),
            status = StepStatus.ACTIVE
          )
        )
      )
    )
    val searchableToTestWith = searchConverterService
      .asSearchableLearningPath(
        domainLearningPath,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          None
        )
      )
      .get

    val searchableFields = searchableToTestWith.asJson
    val fields           = getFields(searchableFields, None, Seq("domainObject"))
    val mapping          = learningPathIndexService.getMapping

    val staticMappingFields  = getMappingFields(mapping.properties, None)
    val dynamicMappingFields = mapping.templates.map(_.name)
    for (field <- fields) {
      val hasStatic  = staticMappingFields.contains(field)
      val hasDynamic = dynamicMappingFields.contains(field)

      if (!(hasStatic || hasDynamic)) {
        fail(s"'$field' was not found in mapping, i think you would want to add it to the index mapping?")
      }
    }
  }
}

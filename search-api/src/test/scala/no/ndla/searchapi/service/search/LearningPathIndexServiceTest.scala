/*
 * Part of NDLA search-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import io.circe.syntax.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{Responsible, Title}
import no.ndla.common.model.domain.learningpath.{
  EmbedType,
  EmbedUrl,
  LearningStep,
  LearningpathCopyright,
  StepStatus,
  StepType,
  Description as LPDescription
}
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.service.ConverterService
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.util.Success

class LearningPathIndexServiceTest extends ElasticsearchIntegrationSuite with UnitSuite with TestEnvironment {

  override implicit lazy val e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  override implicit lazy val searchLanguage: SearchLanguage                     = new SearchLanguage
  override implicit lazy val converterService: ConverterService                 = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService     = new SearchConverterService
  override implicit lazy val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
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
            articleId = None,
            `type` = StepType.TEXT,
            copyright = Some(
              LearningpathCopyright(
                license = "hei",
                contributors = Seq.empty
              )
            ),
            status = StepStatus.ACTIVE
          )
        )
      ),
      responsible = Some(
        Responsible(
          "yolo",
          NDLADate.now()
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

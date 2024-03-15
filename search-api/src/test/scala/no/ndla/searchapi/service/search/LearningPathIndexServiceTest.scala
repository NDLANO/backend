/*
 * Part of NDLA search-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import io.circe.syntax._
import no.ndla.common.model.domain.Title
import no.ndla.common.model.domain.learningpath.{EmbedType, EmbedUrl}
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.searchapi.model.domain.learningpath.{Description, LearningStep, StepStatus, StepType}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.scalatest.Outcome

import scala.util.Failure

class LearningPathIndexServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    elasticSearchContainer match {
      case Failure(ex) =>
        println(s"Elasticsearch container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }

    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    learningPathIndexService.deleteIndexAndAlias()
    learningPathIndexService.createIndexWithGeneratedName
  }

  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  test("That mapping contains every field after serialization") {
    val domainLearningPath = TestData.learningPath1.copy(
      learningsteps = List(
        LearningStep(
          id = Some(1L),
          revision = Some(1),
          externalId = Some("hei"),
          learningPathId = Some(1L),
          seqNo = 1,
          title = Seq(Title("hei", "nb")),
          description = Seq(Description("hei", "nb")),
          embedUrl = Seq(EmbedUrl("hei", "nb", EmbedType.OEmbed)),
          `type` = StepType.TEXT,
          license = Some("hei"),
          showTitle = false,
          status = StepStatus.ACTIVE
        )
      )
    )
    val searchableToTestWith = searchConverterService
      .asSearchableLearningPath(
        domainLearningPath,
        Some(TestData.taxonomyTestBundle)
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

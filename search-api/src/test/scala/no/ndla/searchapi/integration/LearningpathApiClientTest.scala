/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.AuthUser
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.model.domain.{DomainDumpResults, Language, Tag, Title}
import no.ndla.searchapi.model.search.LanguageValue
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.CC_BY
import no.ndla.searchapi.SearchApiProperties.DefaultLanguage
import org.joda.time.DateTime
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.write

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class LearningpathApiClientTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats =
    org.json4s.DefaultFormats +
      new EnumNameSerializer(ArticleStatus) +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType) +
      new EnumNameSerializer(StepStatus) +
      new EnumNameSerializer(EmbedType) +
      new EnumNameSerializer(LearningResourceType) ++
      org.json4s.ext.JodaTimeSerializers.all

  override val ndlaClient = new NdlaClient
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  // Pact CDC imports
  import com.itv.scalapact.ScalaPactForger._
  import com.itv.scalapact.circe13._
  import com.itv.scalapact.http4s21._

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap = Map("Authorization" -> s"Bearer $exampleToken")

  test("that dumping learningpaths returns learningpaths in serializable format") {

    val today = new DateTime(0)
    withFrozenTime(today) {

      val domainLearningStep1 = domain.learningpath.LearningStep(
        None,
        None,
        None,
        None,
        1,
        List(domain.Title("Step1Title", "nb")),
        List(domain.learningpath.Description("Step1Description", "nb")),
        List(),
        domain.learningpath.StepType.INTRODUCTION,
        None
      )

      val domainLearningStep2 = domain.learningpath.LearningStep(
        None,
        None,
        None,
        None,
        2,
        List(domain.Title("Step2Title", "nb")),
        List(domain.learningpath.Description("Step2Description", "nb")),
        List(),
        domain.learningpath.StepType.TEXT,
        None
      )

      val learningPath = domain.learningpath.LearningPath(
        Some(1),
        Some(1),
        None,
        None,
        List(domain.Title("tittel", DefaultLanguage)),
        List(domain.learningpath.Description("deskripsjon", DefaultLanguage)),
        None,
        Some(60),
        domain.learningpath.LearningPathStatus.PUBLISHED,
        domain.learningpath.LearningPathVerificationStatus.CREATED_BY_NDLA,
        today,
        List(domain.Tag(List("tag"), DefaultLanguage)),
        "me",
        domain.learningpath.Copyright(CC_BY.toString, List.empty),
        List(domainLearningStep1, domainLearningStep2)
      )

      val expectedResult = DomainDumpResults[domain.learningpath.LearningPath](
        totalCount = 10,
        page = 1,
        pageSize = 250,
        results = Seq(learningPath)
      )

      forgePact
        .between("search-api")
        .and("learningpath-api")
        .addInteraction(
          interaction
            .description("Dumping learningpaths returns learningpaths in expected format")
            .given("learningpaths")
            .uponReceiving(
              method = GET,
              path = "/intern/dump/learningpath",
              query = None,
              headers = authHeaderMap,
              body = None,
              matchingRules = None
            )
            .willRespondWith(
              status = 200,
              headers = Map.empty,
              body = write(expectedResult),
              matchingRules = None
            )
        )
        .runConsumerTest(mockConfig => {
          AuthUser.setHeader(s"Bearer $exampleToken")
          val learningPathApiClient = new LearningPathApiClient(mockConfig.baseUrl)

          implicit val ec = ExecutionContext.global
          val chunks = learningPathApiClient.getChunks[domain.learningpath.LearningPath].toList
          val fetchedLearningPath = Await.result(chunks.head, Duration.Inf).get.head

          val searchable =
            searchConverterService.asSearchableLearningPath(fetchedLearningPath, TestData.taxonomyTestBundle)

          searchable.isSuccess should be(true)
          searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "tittel")))
          searchable.get.description.languageValues should be(Seq(LanguageValue("nb", "deskripsjon")))
          searchable.get.learningsteps.head.title.languageValues should be(Seq(LanguageValue("nb", "Step1Title")))
          searchable.get.learningsteps.last.title.languageValues should be(Seq(LanguageValue("nb", "Step2Title")))
        })
    }
  }
}

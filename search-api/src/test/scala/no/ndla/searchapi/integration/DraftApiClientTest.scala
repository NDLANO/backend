/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import java.util.Date

import no.ndla.network.AuthUser
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.draft.{ArticleStatus, Copyright}
import no.ndla.mapping.License.CC_BY
import no.ndla.searchapi.model.domain.DomainDumpResults
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.learningpath.{
  EmbedType,
  LearningPathStatus,
  LearningPathVerificationStatus,
  StepStatus,
  StepType
}
import no.ndla.searchapi.model.search.LanguageValue
import org.joda.time.DateTime
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.write
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.Success

class DraftApiClientTest extends UnitSuite with TestEnvironment {
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
  override val searchConverterService = new SearchConverterService

  // Pact CDC imports
  import com.itv.scalapact.ScalaPactForger._
  import com.itv.scalapact.circe13._
  import com.itv.scalapact.http4s21._

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap = Map("Authorization" -> s"Bearer $exampleToken")

  test("that dumping drafts returns drafts in serializable format") {

    val today = new DateTime(0)
    withFrozenTime(today) {

      val draft = domain.draft.Draft(
        Some(1),
        None,
        domain.draft.Status(domain.draft.ArticleStatus.DRAFT, Set.empty),
        Seq(domain.Title("title", "nb")),
        Seq(domain.article.ArticleContent("content", "nb")),
        Some(
          Copyright(
            Some(CC_BY.toString),
            Some(""),
            List.empty,
            List.empty,
            List.empty,
            None,
            None,
            None
          )),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        Seq.empty,
        Seq(domain.article.MetaDescription("meta description", "nb")),
        Seq.empty,
        today,
        today,
        "ndalId54321",
        today,
        domain.article.LearningResourceType.Article,
        List.empty,
        List.empty,
        Seq.empty
      )

      val expectedResult = DomainDumpResults[domain.draft.Draft](
        totalCount = 10,
        page = 1,
        pageSize = 250,
        results = Seq(draft)
      )

      forgePact
        .between("search-api")
        .and("draft-api")
        .addInteraction(
          interaction
            .description("Dumping drafts returns drafts in expected format")
            .given("articles")
            .uponReceiving(
              method = GET,
              path = "/intern/dump/article",
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
          val draftApiClient = new DraftApiClient(mockConfig.baseUrl)

          implicit val ec = ExecutionContext.global
          val chunks = draftApiClient.getChunks[domain.draft.Draft].toList
          val fetchedDraft = Await.result(chunks.head, Duration.Inf).get.head
          val searchable = searchConverterService.asSearchableDraft(fetchedDraft,
                                                                    TestData.taxonomyTestBundle,
                                                                    TestData.emptyGrepBundle)

          searchable.isSuccess should be(true)
          searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "title")))
          searchable.get.content.languageValues should be(Seq(LanguageValue("nb", "content")))
        })
    }
  }
}

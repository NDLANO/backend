/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.AuthUser
import no.ndla.searchapi.model.api.MetaImage
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.article.{
  ArticleContent,
  ArticleMetaImage,
  Availability,
  Copyright,
  LearningResourceType,
  MetaDescription
}
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.model.domain.{DomainDumpResults, Tag, Title}
import no.ndla.searchapi.model.search.LanguageValue
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.joda.time.DateTime
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.write

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class ArticleApiClientTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats =
    org.json4s.DefaultFormats +
      new EnumNameSerializer(ArticleStatus) +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType) +
      new EnumNameSerializer(StepStatus) +
      new EnumNameSerializer(EmbedType) +
      new EnumNameSerializer(LearningResourceType) +
      new EnumNameSerializer(Availability) ++
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

  test("that dumping articles returns articles in serializable format") {

    val today = new DateTime(0)
    withFrozenTime(today) {

      val article = domain.article.Article(
        Option(2),
        None,
        Seq(Title("title", "nb")),
        Seq(ArticleContent("content", "nb")),
        Copyright("CC-BY-4.0", "", Seq(), Seq(), Seq(), None, None, None),
        Seq(Tag(Seq("tag"), "nb")),
        Seq(),
        Seq(),
        Seq(),
        Seq(MetaDescription("meta description", "nb")),
        Seq(ArticleMetaImage("11", "alt", "nb")),
        today,
        today,
        "ndalId54321",
        today,
        LearningResourceType.Article,
        grepCodes = Seq(),
        conceptIds = Seq(),
        availability = Availability.everyone,
        relatedContent = Seq.empty
      )

      val expectedResult = DomainDumpResults[domain.article.Article](
        totalCount = 10,
        page = 1,
        pageSize = 250,
        results = Seq(article)
      )

      forgePact
        .between("search-api")
        .and("article-api")
        .addInteraction(
          interaction
            .description("Dumping articles returns articles in expected format")
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
          val articleApiClient = new ArticleApiClient(mockConfig.baseUrl)

          implicit val ec = ExecutionContext.global
          val chunks = articleApiClient.getChunks[domain.article.Article].toList
          val fetchedArticle = Await.result(chunks.head, Duration.Inf).get.head
          val searchable = searchConverterService.asSearchableArticle(fetchedArticle,
                                                                      TestData.taxonomyTestBundle,
                                                                      TestData.emptyGrepBundle)

          searchable.isSuccess should be(true)
          searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "title")))
          searchable.get.content.languageValues should be(Seq(LanguageValue("nb", "content")))
        })
    }
  }
}

/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.model.{Sort, domain}
import no.ndla.audioapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.common.model.{domain => common}
import no.ndla.scalatestsuite.IntegrationSuite

import scala.util.Success

class SeriesSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override val seriesSearchService = new SeriesSearchService
  override val seriesIndexService: SeriesIndexService = new SeriesIndexService {
    override val indexShards = 1
  }
  override val searchConverterService = new SearchConverterService
  override val converterService       = new ConverterService

  val seriesToIndex: Seq[Series] = Seq(
    TestData.SampleSeries.copy(
      id = 1,
      title = Seq(common.Title("Lyd med epler", "nb"), common.Title("Sound with apples", "en")),
      description = Seq(domain.Description("megabeskrivelse", "nb"), domain.Description("giant description", "en"))
    ),
    TestData.SampleSeries.copy(
      id = 2,
      title = Seq(common.Title("Lyd med tiger", "nb"))
    ),
    TestData.SampleSeries.copy(
      id = 3,
      title = Seq(common.Title("Lyd på språket Mixtepec Mixtec uten analyzer", "mix"))
    )
  )

  val settings: SeriesSearchSettings = SeriesSearchSettings(
    query = None,
    language = None,
    page = None,
    pageSize = None,
    sort = Sort.ByIdAsc,
    shouldScroll = false,
    fallback = false
  )

  override def beforeEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      seriesIndexService.createIndexAndAlias()
    }
  }

  override def afterEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      seriesIndexService.deleteIndexAndAlias()
    }
  }

  def indexAndWait(series: Seq[domain.Series]): Unit = {
    series.map(s => seriesIndexService.indexDocument(s).get)
    blockUntil(() => seriesIndexService.countDocuments == series.size)
  }

  test("That query search works as expected") {
    indexAndWait(seriesToIndex)

    val Success(result1) = seriesSearchService.matchingQuery(settings.copy(query = Some("tiger")))
    result1.results.map(_.id) should be(Seq(2))

    val Success(result2) = seriesSearchService.matchingQuery(settings.copy(query = Some("Lyd med")))
    result2.results.map(_.id) should be(Seq(1, 2, 3))

    val Success(result3) = seriesSearchService.matchingQuery(settings.copy(query = Some("epler")))
    result3.results.map(_.id) should be(Seq(1))

    val Success(result4) =
      seriesSearchService.matchingQuery(settings.copy(query = Some("mixtepec"), language = Some("mix")))
    result4.results.map(_.id) should be(Seq(3))
  }

  test("That descriptions are searchable") {
    indexAndWait(seriesToIndex)

    val Success(result1) = seriesSearchService.matchingQuery(settings.copy(query = Some("megabeskrivelse")))
    result1.results.map(_.id) should be(Seq(1))

    val Success(result2) =
      seriesSearchService.matchingQuery(settings.copy(query = Some("description"), language = Some("en")))
    result2.results.map(_.id) should be(Seq(1))

  }

  test("That fallback searching includes languages outside the search") {
    val seriesToIndex = Seq(
      TestData.SampleSeries.copy(
        id = 1,
        title = Seq(common.Title("Lyd med epler", "nb"), common.Title("Sound with apples", "en")),
        description = Seq(domain.Description("megabeskrivelse", "nb"), domain.Description("giant description", "en"))
      ),
      TestData.SampleSeries.copy(
        id = 2,
        title = Seq(common.Title("Lyd med tiger", "nb")),
        description = Seq(domain.Description("megabeskrivelse", "nb"))
      ),
      TestData.SampleSeries.copy(
        id = 3,
        title = Seq(common.Title("Lyd på språket Mixtepec Mixtec uten analyzer", "mix")),
        description = Seq(domain.Description("descriptos", "mix"))
      )
    )
    indexAndWait(seriesToIndex)

    val Success(result1) = seriesSearchService.matchingQuery(
      settings.copy(
        query = None,
        fallback = true,
        language = Some("nb"),
        sort = Sort.ByIdAsc
      )
    )
    result1.results.length should be(seriesToIndex.length)
    result1.results.map(_.id) should be(Seq(1, 2, 3))
    result1.results.head.title.language should be("nb")
    result1.results.last.title.language should be("mix")

    val Success(result2) = seriesSearchService.matchingQuery(
      settings.copy(
        query = None,
        fallback = true,
        language = Some("en"),
        sort = Sort.ByIdAsc
      )
    )
    result2.results.length should be(seriesToIndex.length)
    result2.results.map(_.id) should be(Seq(1, 2, 3))
    result2.results.head.title.language should be("en")
    result2.results(1).title.language should be("nb")
    result2.results.last.title.language should be("mix")
  }
}

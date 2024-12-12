/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import cats.implicits.catsSyntaxOptionId
import no.ndla.network.tapir.NonEmptyString
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.searchapi.TestEnvironment
import no.ndla.searchapi.controller.parameters.GrepSearchInputDTO
import no.ndla.searchapi.model.api.grep.GrepSortDTO.{ByCodeAsc, ByCodeDesc}
import no.ndla.searchapi.model.grep.{GrepBundle, GrepElement, GrepTitle}

class GrepSearchServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))

  override val grepIndexService: GrepIndexService = new GrepIndexService {
    override val indexShards = 1
  }
  override val grepSearchService      = new GrepSearchService
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  override def beforeEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      grepIndexService.createIndexAndAlias().get
    }
  }

  override def afterEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      grepIndexService.deleteIndexAndAlias()
    }
  }

  val grepTestBundle: GrepBundle = GrepBundle(
    kjerneelementer = List(
      GrepElement(
        "KE12",
        Seq(GrepTitle("default", "Utforsking og problemløysing"), GrepTitle("nob", "Utforsking og problemløsning"))
      ),
      GrepElement(
        "KE34",
        Seq(GrepTitle("default", "Abstraksjon og generalisering"), GrepTitle("nob", "Abstraksjon og generalisering"))
      )
    ),
    kompetansemaal = List(
      GrepElement(
        "KM123",
        Seq(
          GrepTitle("default", "bruke ulike kilder på en kritisk, hensiktsmessig og etterrettelig måte"),
          GrepTitle("nob", "bruke ulike kilder på en kritisk, hensiktsmessig og etterrettelig måte")
        )
      )
    ),
    kompetansemaalsett = List.empty,
    tverrfagligeTemaer = List(
      GrepElement(
        "TT2",
        Seq(GrepTitle("default", "Demokrati og medborgerskap"), GrepTitle("nob", "Demokrati og medborgerskap"))
      )
    )
  )

  val emptyInput: GrepSearchInputDTO = GrepSearchInputDTO(
    codes = None,
    language = None,
    page = None,
    pageSize = None,
    query = None,
    prefixFilter = None,
    sort = None
  )

  test("That searching for all grep codes works as expected") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val result = grepSearchService.searchGreps(emptyInput).get
    result.results.map(_.code).sorted should be(grepTestBundle.grepContext.map(_.kode).sorted)
  }

  test("That querying grep codes with prefixes returns nothing") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val result = grepSearchService
      .searchGreps(emptyInput.copy(query = NonEmptyString.fromString("kakepenger"), prefixFilter = Some(List("TT"))))
      .get

    result.results.map(_.code).sorted should be(Seq.empty)
  }

  test("That searching for all grep prefixes works as expected") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val input  = emptyInput.copy(prefixFilter = Some(List("KE")))
    val result = grepSearchService.searchGreps(input).get
    result.results.map(_.code).sorted should be(List("KE12", "KE34"))
  }

  test("That querying the grep codes searches titles") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val input  = emptyInput.copy(query = NonEmptyString.fromString("hensiktsmessig"))
    val result = grepSearchService.searchGreps(input).get
    result.results.map(_.code).sorted should be(List("KM123"))
  }

  test("That looking up based on id works as expected") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val input  = emptyInput.copy(codes = Some(List("KM123", "ENUKJENT123")))
    val result = grepSearchService.searchGreps(input).get
    result.results.map(_.code).sorted should be(List("KM123"))

    val input2  = emptyInput.copy(codes = Some(List("km123", "ENUKJENT123")))
    val result2 = grepSearchService.searchGreps(input2).get
    result2.results.map(_.code).sorted should be(List("KM123"))
  }

  test("That querying based on id works as expected") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val input  = emptyInput.copy(query = NonEmptyString.fromString("KM123"))
    val result = grepSearchService.searchGreps(input).get
    result.results.map(_.code).sorted should be(List("KM123"))
  }

  test("That sorting works as expected") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val result1 = grepSearchService.searchGreps(emptyInput.copy(sort = Some(ByCodeAsc))).get
    result1.results.map(_.code) should be(List("KE12", "KE34", "KM123", "TT2"))

    val result2 = grepSearchService.searchGreps(emptyInput.copy(sort = Some(ByCodeDesc))).get
    result2.results.map(_.code) should be(List("TT2", "KM123", "KE34", "KE12"))
  }

  test("That prefix filter is case insensitive") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val result1 = grepSearchService.searchGreps(emptyInput.copy(prefixFilter = Some(List("ke")))).get
    result1.results.map(_.code) should be(List("KE12", "KE34"))

    val result2 = grepSearchService.searchGreps(emptyInput.copy(query = NonEmptyString.fromString("ke"))).get
    result2.results.map(_.code) should be(List("KE12", "KE34"))

    val result3 = grepSearchService.searchGreps(emptyInput.copy(prefixFilter = Some(List("KE")))).get
    result3.results.map(_.code) should be(List("KE12", "KE34"))

    val result4 = grepSearchService.searchGreps(emptyInput.copy(query = NonEmptyString.fromString("KE"))).get
    result4.results.map(_.code) should be(List("KE12", "KE34"))
  }
}

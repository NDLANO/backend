/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import cats.implicits.catsSyntaxOptionId
import no.ndla.network.tapir.NonEmptyString
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.searchapi.TestEnvironment
import no.ndla.searchapi.controller.parameters.GrepSearchInputDTO
import no.ndla.searchapi.model.api.grep.GrepSortDTO.{ByCodeAsc, ByCodeDesc}
import no.ndla.searchapi.model.grep.{
  BelongsToObj,
  GrepBundle,
  GrepKjerneelement,
  GrepKompetansemaal,
  GrepLaererplan,
  GrepTitle,
  GrepTverrfagligTema,
  GrepTextObj
}

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
      GrepKjerneelement(
        "KE12",
        GrepTextObj(
          List(GrepTitle("default", "Utforsking og problemløysing"), GrepTitle("nob", "Utforsking og problemløsning"))
        ),
        GrepTextObj(List(GrepTitle("default", ""))),
        BelongsToObj("LP1", "Dette er LP1")
      ),
      GrepKjerneelement(
        "KE34",
        GrepTextObj(
          List(GrepTitle("default", "Abstraksjon og generalisering"), GrepTitle("nob", "Abstraksjon og generalisering"))
        ),
        GrepTextObj(List(GrepTitle("default", ""))),
        BelongsToObj("LP2", "Dette er LP2")
      )
    ),
    kompetansemaal = List(
      GrepKompetansemaal(
        kode = "KM123",
        tittel = GrepTextObj(
          List(
            GrepTitle("default", "bruke ulike kilder på en kritisk, hensiktsmessig og etterrettelig måte"),
            GrepTitle("nob", "bruke ulike kilder på en kritisk, hensiktsmessig og etterrettelig måte")
          )
        ),
        `tilhoerer-laereplan` = BelongsToObj("LP2", "Dette er LP2"),
        `tilhoerer-kompetansemaalsett` = BelongsToObj("KE200", "Kompetansemaalsett"),
        `tilknyttede-tverrfaglige-temaer` = List(),
        `tilknyttede-kjerneelementer` = List(),
        `gjenbruk-av` = None
      )
    ),
    kompetansemaalsett = List.empty,
    tverrfagligeTemaer = List(
      GrepTverrfagligTema(
        "TT2",
        Seq(GrepTitle("default", "Demokrati og medborgerskap"), GrepTitle("nob", "Demokrati og medborgerskap"))
      )
    ),
    laereplaner = List(
      GrepLaererplan(
        "LP1",
        GrepTextObj(List(GrepTitle("default", "Læreplan i norsk"), GrepTitle("nob", "Læreplan i norsk"))),
        List.empty
      ),
      GrepLaererplan(
        "LP2",
        GrepTextObj(List(GrepTitle("default", "Læreplan i engelsk"), GrepTitle("nob", "Læreplan i engelsk"))),
        List.empty
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
    result1.results.map(_.code) should be(List("KE12", "KE34", "KM123", "LP1", "LP2", "TT2"))

    val result2 = grepSearchService.searchGreps(emptyInput.copy(sort = Some(ByCodeDesc))).get
    result2.results.map(_.code) should be(List("TT2", "LP2", "LP1", "KM123", "KE34", "KE12"))
  }

  test("That prefix filter is case insensitive") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val result1 = grepSearchService.searchGreps(emptyInput.copy(prefixFilter = Some(List("ke")))).get
    result1.results.map(_.code) should be(List("KE12", "KE34"))

    val result2 = grepSearchService.searchGreps(emptyInput.copy(prefixFilter = Some(List("KE")))).get
    result2.results.map(_.code) should be(List("KE12", "KE34"))
  }

  test("That query code search is case insensitive") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val result1 = grepSearchService.searchGreps(emptyInput.copy(query = NonEmptyString.fromString("KE"))).get
    result1.results.map(_.code) should be(List("KE12", "KE34"))

    val result2 = grepSearchService.searchGreps(emptyInput.copy(query = NonEmptyString.fromString("ke"))).get
    result2.results.map(_.code) should be(List("KE12", "KE34"))
  }

  test("That searching for a læreplan helps out") {
    grepIndexService.indexDocuments(1.some, Some(grepTestBundle)).get
    blockUntil(() => grepIndexService.countDocuments == grepTestBundle.grepContext.size)

    val result1 = grepSearchService
      .searchGreps(
        emptyInput.copy(
          query = NonEmptyString.fromString("og LP2"),
          prefixFilter = Some(List("KE"))
        )
      )
      .get
    result1.results.map(_.code) should be(List("KE34", "KE12"))

    val result2 = grepSearchService
      .searchGreps(
        emptyInput.copy(
          query = NonEmptyString.fromString("og LP1"),
          prefixFilter = Some(List("KE"))
        )
      )
      .get
    result2.results.map(_.code) should be(List("KE12", "KE34"))

  }

  test("That we are able to extract codes from the query") {
    grepSearchService.extractCodesFromQuery("heisann KE12 KE34 KM123 LP1 lille luring LP2 TT2 LMI01-05") should be(
      Set("KE12", "KE34", "KM123", "LP1", "LP2", "TT2", "LMI01-05")
    )
  }

  test("That we are able to extract codeprefixes from the query") {
    grepSearchService.extractCodePrefixesFromQuery("heisann KE LMI APE02- APE05-5") should be(
      Set("KE", "LMI", "APE02", "APE05-5")
    )
  }
}

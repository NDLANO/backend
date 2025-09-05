/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.RequestSuccess
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder.{Asc, Desc}
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchRequest, SearchResponse}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.common.model.api.search.SearchType
import no.ndla.language.Language.AllLanguages
import no.ndla.language.model.Iso639
import no.ndla.search.{BaseIndexService, NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.controller.parameters.GrepSearchInputDTO
import no.ndla.searchapi.model.api.grep.GrepSortDTO.*
import no.ndla.searchapi.model.api.grep.{GrepResultDTO, GrepSearchResultsDTO, GrepSortDTO}
import no.ndla.searchapi.model.grep.{
  GrepKjerneelement,
  GrepKompetansemaal,
  GrepKompetansemaalSett,
  GrepLaererplan,
  GrepTverrfagligTema
}
import no.ndla.searchapi.model.search.SearchableGrepElement

import scala.util.{Failure, Success, Try, boundary}

class GrepSearchService(using
    props: Props,
    grepIndexService: GrepIndexService,
    e4sClient: NdlaE4sClient,
    searchConverterService: SearchConverterService,
    searchLanguage: SearchLanguage
) extends SearchService
    with StrictLogging {
  override val searchIndex: List[String]             = List(SearchType.Grep).map(props.SearchIndex)
  override val indexServices: List[BaseIndexService] = List(grepIndexService)

  def grepSortDefinition(maybeSort: Option[GrepSortDTO], language: String): FieldSort = maybeSort match {
    case Some(ByRelevanceAsc)         => sortField("_score", Asc, missingLast = false)
    case Some(ByRelevanceDesc) | None => sortField("_score", Desc, missingLast = false)
    case Some(ByTitleAsc)             => defaultSort("defaultTitle", "title", Asc, language)
    case Some(ByTitleDesc)            => defaultSort("defaultTitle", "title", Desc, language)
    case Some(ByCodeAsc)              => sortField("code", Asc, missingLast = false)
    case Some(ByCodeDesc)             => sortField("code", Desc, missingLast = false)
    case Some(ByStatusAsc)            => sortField("status", Asc, missingLast = false)
    case Some(ByStatusDesc)           => sortField("status", Desc, missingLast = false)
  }

  protected def buildCodeQueries(codePrefixes: Set[String], codes: Set[String]): Option[Query] = {

    val prefixQueries = (codePrefixes ++ codes).toList.flatMap { prefix =>
      List(
        prefixQuery("code", prefix).boost(50),
        prefixQuery("belongsTo", prefix).boost(50)
      )
    }

    val codeQueries = codes.flatMap { query =>
      List(
        matchQuery("code", query).boost(50),
        termQuery("code", query).boost(50),
        matchQuery("belongsTo", query).boost(50),
        termQuery("belongsTo", query).boost(50)
      )
    }

    val queries = prefixQueries ++ codeQueries
    Option.when(queries.nonEmpty) { boolQuery().should(queries) }
  }

  def extractCodesFromQuery(query: String): Set[String] = {
    val regex = """\b([A-Za-z]{2,3}\d{1,4}(?:-\d{1,4})?)\b""".r
    regex.findAllIn(query).toSet
  }

  def extractCodePrefixesFromQuery(query: String): Set[String] = {
    val regex = """\b([A-Za-z]{2,3}(\d{1,4})?(?:-\d{1,4})?)\b""".r
    regex.findAllIn(query).toSet
  }

  protected def buildQuery(input: GrepSearchInputDTO, searchLanguage: String): Query = {
    val query = input.query match {
      case Some(q) =>
        val codes        = extractCodesFromQuery(q.underlying)
        val codePrefixes = extractCodePrefixesFromQuery(q.underlying)
        val codeQueries  = buildCodeQueries(codePrefixes, codes)
        val titleQuery   = languageQuery(q, "title", 6, searchLanguage)

        boolQuery()
          .withShould(titleQuery)
          .withShould(codeQueries)
          .minimumShouldMatch(1)
      case None => boolQuery()
    }
    query.filter(getFilters(input))
  }

  private def getFilters(input: GrepSearchInputDTO): List[Query] =
    List(
      idsFilter(input),
      prefixFilter(input)
    ).flatten

  private def idsFilter(input: GrepSearchInputDTO): Option[Query] = input.codes match {
    case Some(ids) if ids.nonEmpty => termsQuery("code", ids).some
    case _                         => None
  }

  private def prefixFilter(input: GrepSearchInputDTO): Option[Query] = input.prefixFilter match {
    case Some(prefixes) if prefixes.nonEmpty =>
      Some(
        boolQuery().should(
          prefixes.map(prefix => prefixQuery("code", prefix))
        )
      )
    case _ => None
  }

  private def getSingleCodeById(code: String): Try[Option[SearchableGrepElement]] = {
    val searchToExecute = search(searchIndex)
      .query(termQuery("code", code))
      .trackTotalHits(true)

    e4sClient.execute(searchToExecute).flatMap { response =>
      withGrepHits(response, hit => hitToSearchable(hit)).flatMap {
        case head :: Nil => Success(Some(head))
        case Nil         => Success(None)
        case _           => Failure(new RuntimeException(s"Multiple hits for code $code"))
      }
    }
  }

  private def executeAsSearchableGreps(searchToExecute: SearchRequest) = {
    e4sClient.execute(searchToExecute).flatMap { response =>
      withGrepHits(response, hit => hitToSearchable(hit))
    }
  }

  def getCodesById(codes: List[String]): Try[List[SearchableGrepElement]] = {
    val filter          = termsQuery("code", codes)
    val searchToExecute = search(searchIndex)
      .query(boolQuery().filter(filter))
      .from(0)
      .size(codes.size)
      .trackTotalHits(true)

    executeAsSearchableGreps(searchToExecute)
  }

  def searchGreps(input: GrepSearchInputDTO): Try[GrepSearchResultsDTO] = permitTry {
    val searchLanguage = input.language match {
      case Some(lang) if Iso639.get(lang).isSuccess => lang
      case _                                        => AllLanguages
    }
    val searchPage     = input.page.getOrElse(1)
    val searchPageSize = input.pageSize.getOrElse(10)
    val pagination     = getStartAtAndNumResults(page = searchPage, pageSize = searchPageSize).?
    val sort           = grepSortDefinition(input.sort, searchLanguage)
    val filteredQuery  = buildQuery(input, searchLanguage)

    val searchToExecute = search(searchIndex)
      .query(filteredQuery)
      .from(pagination.startAt)
      .size(pagination.pageSize)
      .trackTotalHits(true)
      .sortBy(sort)

    e4sClient.execute(searchToExecute).flatMap { response =>
      withGrepHits(response, hit => hitToResult(hit, searchLanguage)).map { results =>
        GrepSearchResultsDTO(
          totalCount = response.result.totalHits,
          page = pagination.page,
          pageSize = searchPageSize,
          language = searchLanguage,
          results = results
        )
      }
    }
  }

  def getReuseOf(code: String): Try[List[SearchableGrepElement]] = {
    val filter          = termQuery("gjenbrukAv", code)
    val searchToExecute = search(searchIndex)
      .query(boolQuery().filter(filter))
      .from(0)
      .size(1000)
      .trackTotalHits(true)
    executeAsSearchableGreps(searchToExecute)
  }

  def hitToSearchable(hit: SearchHit): Try[SearchableGrepElement] = {
    val jsonString = hit.sourceAsString
    CirceUtil.tryParseAs[SearchableGrepElement](jsonString)
  }

  private def hitToResult(hit: SearchHit, language: String): Try[GrepResultDTO] = permitTry {
    val searchable = hitToSearchable(hit).?
    GrepResultDTO.fromSearchable(searchable, language)
  }

  private def withGrepHits[T](response: RequestSuccess[SearchResponse], f: SearchHit => Try[T]): Try[List[T]] = {
    response.result.hits.hits.toList.traverse { hit => f(hit) }
  }

  private def getCoreElementReplacement(core: GrepKjerneelement): Try[String] = permitTry {
    boundary {
      val lpCode  = core.`tilhoerer-laereplan`.kode
      val foundLp = getSingleCodeById(lpCode) match {
        case Success(Some(lp)) => lp
        case Failure(ex)       => boundary.break(Failure(ex))
        case Success(None)     =>
          logger.warn(s"Could not find læreplan for core element: ${core.kode} (LP: $lpCode)")
          boundary.break(Success(core.kode))
      }

      val domainObject = foundLp.domainObject match {
        case lp: GrepLaererplan => lp
        case _                  =>
          val msg =
            s"Got unexpected domain object when looking up læreplan (${foundLp.code}) for replacement for core element (${core.kode})"
          logger.error(msg)
          boundary.break(Failure(new RuntimeException(msg)))
      }

      getLaererplanReplacement(domainObject) match {
        case None                  => Success(core.kode)
        case Some(replacementPlan) =>
          val elementsInPlan   = elementsWithLpCode(replacementPlan).?
          val foundReplacement = elementsInPlan.find { x =>
            core.tittel.tekst == x.domainObject.getTitle
          }
          Success(foundReplacement.map(_.code).getOrElse(core.kode))
      }
    }
  }

  private def elementsWithLpCode(code: String): Try[List[SearchableGrepElement]] = {
    val filter          = termQuery("laereplanCode", code)
    val searchToExecute = search(searchIndex)
      .query(boolQuery().filter(filter))
      .from(0)
      .size(1000)
      .trackTotalHits(true)
    executeAsSearchableGreps(searchToExecute)
  }

  def getKompetansemaalReplacement(goal: GrepKompetansemaal): Try[String] = permitTry {
    val reuseOf = getReuseOf(goal.kode).?
    reuseOf match {
      case head :: Nil =>
        logger.info(s"Replacing ${goal.kode} with ${head.code}")
        Success(head.code)
      case Nil =>
        Success(goal.kode)
      case _ =>
        logger.warn(s"Multiple replacements for goal ${goal.kode}")
        Success(goal.kode)
    }
  }

  private def getLaererplanReplacement(plan: GrepLaererplan): Option[String] = {
    if (plan.`erstattes-av`.size == 1) {
      Some(plan.`erstattes-av`.head.kode)
    } else if (plan.`erstattes-av`.nonEmpty) {
      None
    } else {
      logger.warn(s"Multiple replacements for plan ${plan.kode}")
      None
    }
  }

  private def findReplacementCode(code: SearchableGrepElement): Try[(String, String)] = {
    val result = code.domainObject match {
      case x: GrepKompetansemaalSett => Success(x.kode)
      case x: GrepTverrfagligTema    => Success(x.kode)
      case x: GrepKjerneelement      => getCoreElementReplacement(x)
      case x: GrepKompetansemaal     => getKompetansemaalReplacement(x)
      case x: GrepLaererplan         => Success(getLaererplanReplacement(x).getOrElse(x.kode))
    }

    result.map(r => code.code -> r)
  }

  def getReplacements(codes: List[String]): Try[Map[String, String]] = permitTry {
    val foundOldCodes = getCodesById(codes).?
    val foundAllCodes = foundOldCodes.map(_.code).toSet
    val missingCodes  = codes.toSet.diff(foundAllCodes)
    if (missingCodes.nonEmpty) {
      val msg = s"Not all codes to replace found in search index, missing: [${missingCodes.mkString(",")}]"
      logger.error(msg)
    }

    val convertedCodes   = foundOldCodes.traverse { oc => findReplacementCode(oc) }.?
    val missingCodesList = missingCodes.map(x => x -> x)
    Success((convertedCodes ++ missingCodesList).toMap)
  }
}

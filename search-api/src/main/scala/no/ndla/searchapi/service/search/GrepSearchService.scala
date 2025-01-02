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
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchResponse}
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.language.Language.AllLanguages
import no.ndla.language.model.Iso639
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import no.ndla.searchapi.Props
import no.ndla.searchapi.controller.parameters.GrepSearchInputDTO
import no.ndla.searchapi.model.api.grep.GrepSortDTO.*
import no.ndla.searchapi.model.api.grep.{GrepResultDTO, GrepSearchResultsDTO, GrepSortDTO}
import no.ndla.searchapi.model.search.{SearchType, SearchableGrepElement}

import scala.util.Try

trait GrepSearchService {
  this: Props & SearchService & GrepIndexService & BaseIndexService & Elastic4sClient & SearchConverterService =>
  val grepSearchService: GrepSearchService

  class GrepSearchService extends SearchService {
    import props.SearchIndex
    override val searchIndex: List[String]             = List(SearchType.Grep).map(SearchIndex)
    override val indexServices: List[BaseIndexService] = List(grepIndexService)

    def grepSortDefinition(maybeSort: Option[GrepSortDTO], language: String): FieldSort = maybeSort match {
      case Some(ByRelevanceAsc)         => sortField("_score", Asc, missingLast = false)
      case Some(ByRelevanceDesc) | None => sortField("_score", Desc, missingLast = false)
      case Some(ByTitleAsc)             => defaultSort("defaultTitle", "title", Asc, language)
      case Some(ByTitleDesc)            => defaultSort("defaultTitle", "title", Desc, language)
      case Some(ByCodeAsc)              => sortField("code", Asc, missingLast = false)
      case Some(ByCodeDesc)             => sortField("code", Desc, missingLast = false)
    }

    protected def buildCodeQueries(codePrefixes: Set[String], codes: Set[String]): Option[Query] = {

      val prefixQueries = (codePrefixes ++ codes).toList.flatMap { prefix =>
        List(
          prefixQuery("code", prefix).boost(50),
          prefixQuery("laereplanCode", prefix).boost(50)
        )
      }

      val codeQueries = codes.flatMap { query =>
        List(
          matchQuery("code", query).boost(50),
          termQuery("code", query).boost(50),
          matchQuery("laereplanCode", query).boost(50),
          termQuery("laereplanCode", query).boost(50)
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

    def searchGreps(input: GrepSearchInputDTO): Try[GrepSearchResultsDTO] = {
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
        getGrepHits(response, searchLanguage).map { results =>
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

    private def hitToResult(hit: SearchHit, language: String): Try[GrepResultDTO] = {
      val jsonString = hit.sourceAsString
      val searchable = CirceUtil.tryParseAs[SearchableGrepElement](jsonString).?
      GrepResultDTO.fromSearchable(searchable, language)
    }

    private def getGrepHits(response: RequestSuccess[SearchResponse], language: String): Try[List[GrepResultDTO]] = {
      response.result.hits.hits.toList.traverse { hit => hitToResult(hit, language) }
    }
  }
}

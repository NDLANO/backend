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
import no.ndla.language.Language
import no.ndla.language.Language.{AllLanguages, findByLanguageOrBestEffort}
import no.ndla.language.model.Iso639
import no.ndla.search.model.LanguageValue
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import no.ndla.searchapi.Props
import no.ndla.searchapi.controller.parameters.GrepSearchInputDTO
import no.ndla.searchapi.model.api.TitleDTO
import no.ndla.searchapi.model.api.grep.GrepSortDTO.*
import no.ndla.searchapi.model.api.grep.{GrepResultDTO, GrepSearchResultsDTO, GrepSortDTO}
import no.ndla.searchapi.model.search.{SearchType, SearchableGrepElement}

import scala.util.{Success, Try}

trait GrepSearchService {
  this: Props & SearchService & GrepIndexService & BaseIndexService & Elastic4sClient =>
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

    protected def buildCodeQueries(query: String): Query = {
      boolQuery().should(
        List(
          prefixQuery("code", query).boost(50),
          matchQuery("code", query).boost(10),
          termQuery("code", query).boost(100),
          prefixQuery("laereplanCode", query).boost(50),
          matchQuery("laereplanCode", query).boost(10),
          termQuery("laereplanCode", query).boost(100)
        )
      )
    }

    protected def buildPartialPrefixQuery(query: String): List[Query] = {
      query
        .split(" ")
        .slice(0, 10) // Limit to 10 prefixes for performance reasons
        .flatMap { qp =>
          List(
            prefixQuery("laereplanCode", qp).boost(5),
            prefixQuery("code", qp).boost(10)
          )
        }
        .toList
    }

    protected def buildQuery(input: GrepSearchInputDTO, searchLanguage: String): Query = {
      val query = input.query
        .map { q =>
          val langQueryFunc = (fieldName: String, boost: Double) =>
            buildSimpleStringQueryForField(
              q,
              fieldName,
              boost,
              searchLanguage,
              fallback = true,
              searchDecompounded = true
            )

          val codeQueries          = buildCodeQueries(q.underlying)
          val titleQuery           = langQueryFunc("title", 6)
          val partialPrefixQueries = buildPartialPrefixQuery(q.underlying)
          val onlyCodeQuery        = boolQuery().must(codeQueries).not(titleQuery)

          boolQuery()
            .should(Seq(titleQuery, onlyCodeQuery) ++ partialPrefixQueries)
            .minimumShouldMatch(1)
        }
        .getOrElse(boolQuery())
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

      val sort          = grepSortDefinition(input.sort, searchLanguage)
      val filteredQuery = buildQuery(input, searchLanguage)

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

    def hitToResult(hit: SearchHit, language: String): Try[GrepResultDTO] = {
      val jsonString = hit.sourceAsString
      val searchable = CirceUtil.tryParseAs[SearchableGrepElement](jsonString).?
      val titleLv = findByLanguageOrBestEffort(searchable.title.languageValues, language)
        .getOrElse(LanguageValue(Language.DefaultLanguage, ""))
      val title = TitleDTO(title = titleLv.value, language = titleLv.language)

      Success(
        GrepResultDTO(
          code = searchable.code,
          title = title,
          laereplanCode = searchable.laereplanCode
        )
      )
    }

    def getGrepHits(response: RequestSuccess[SearchResponse], language: String): Try[List[GrepResultDTO]] = {
      response.result.hits.hits.toList.traverse { hit => hitToResult(hit, language) }
    }
  }
}

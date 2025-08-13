/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.controller

import cats.implicits.catsSyntaxEitherId
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.implicits.*
import no.ndla.common.model.api.LanguageCode
import no.ndla.conceptapi.model.api.*
import no.ndla.conceptapi.model.domain.Sort
import no.ndla.conceptapi.model.search.SearchSettings
import no.ndla.conceptapi.service.search.{PublishedConceptSearchService, SearchConverterService}
import no.ndla.conceptapi.service.{ReadService, WriteService}
import no.ndla.conceptapi.Props
import no.ndla.language.Language
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.{DynamicHeaders, TapirController}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait PublishedConceptController {
  this: WriteService & ReadService & PublishedConceptSearchService & SearchConverterService & Props &
    ConceptControllerHelpers & ErrorHandling & TapirController =>
  lazy val publishedConceptController: PublishedConceptController

  class PublishedConceptController extends TapirController {
    import ConceptControllerHelpers.*

    override val serviceName: String         = "concepts"
    override val prefix: EndpointInput[Unit] = "concept-api" / "v1" / serviceName

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getTags,
      getConceptById,
      getAllConcepts,
      postSearchConcepts
    )

    private def scrollSearchOr(scrollId: Option[String], language: LanguageCode)(
        orFunction: => Try[(ConceptSearchResultDTO, DynamicHeaders)]
    ): Try[(ConceptSearchResultDTO, DynamicHeaders)] =
      scrollId match {
        case Some(scroll) if !props.InitialScrollContextKeywords.contains(scroll) =>
          publishedConceptSearchService.scroll(scroll, language.code) match {
            case Success(scrollResult) =>
              val body    = searchConverterService.asApiConceptSearchResult(scrollResult)
              val headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
              Success((body, headers))
            case Failure(ex) => Failure(ex)
          }
        case _ => orFunction
      }

    private def search(
        query: Option[String],
        sort: Option[Sort],
        language: String,
        page: Int,
        pageSize: Int,
        idList: List[Long],
        fallback: Boolean,
        tagsToFilterBy: Set[String],
        exactTitleMatch: Boolean,
        shouldScroll: Boolean,
        embedResource: List[String],
        embedId: Option[String],
        conceptType: Option[String],
        aggregatePaths: List[String]
    ) = {
      val settings = SearchSettings(
        withIdIn = idList,
        searchLanguage = language,
        page = page,
        pageSize = pageSize,
        sort = sort.getOrElse(Sort.ByRelevanceDesc),
        fallback = fallback,
        tagsToFilterBy = tagsToFilterBy,
        exactTitleMatch = exactTitleMatch,
        shouldScroll = shouldScroll,
        embedResource = embedResource,
        embedId = embedId,
        conceptType = conceptType,
        aggregatePaths = aggregatePaths
      )

      val result = query.emptySomeToNone match {
        case Some(q) =>
          publishedConceptSearchService.matchingQuery(q, settings.copy(sort = sort.getOrElse(Sort.ByRelevanceDesc)))
        case None => publishedConceptSearchService.all(settings.copy(sort = sort.getOrElse(Sort.ByTitleDesc)))
      }

      result match {
        case Success(searchResult) =>
          val scrollHeader = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
          val output       = searchConverterService.asApiConceptSearchResult(searchResult)
          Success((output, scrollHeader))
        case Failure(ex) => Failure(ex)
      }

    }

    def getConceptById: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show concept with a specified id")
      .description("Shows the concept for the specified id.")
      .in(pathConceptId)
      .in(language)
      .in(fallback)
      .out(jsonBody[ConceptDTO])
      .errorOut(errorOutputsFor(404))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (conceptId, language, fallback) =>
          readService.publishedConceptWithId(conceptId, language.code, fallback, user)
        }
      }

    def getAllConcepts: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show all concepts")
      .description("Shows all concepts. You can search it too.")
      .in(queryParam)
      .in(conceptIds)
      .in(language)
      .in(pageNo)
      .in(pageSize)
      .in(sort)
      .in(fallback)
      .in(scrollId)
      .in(tagsToFilterBy)
      .in(exactTitleMatch)
      .in(embedResource)
      .in(embedId)
      .in(conceptType)
      .in(aggregatePaths)
      .out(jsonBody[ConceptSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure {
        case (
              query,
              idList,
              language,
              page,
              pageSize,
              sortStr,
              fallback,
              scrollId,
              tagsToFilterBy,
              exactTitleMatch,
              embedResource,
              embedId,
              conceptType,
              aggregatePaths
            ) =>
          scrollSearchOr(scrollId, language) {
            val sort         = Sort.valueOf(sortStr)
            val shouldScroll = scrollId.exists(props.InitialScrollContextKeywords.contains)

            search(
              query,
              sort,
              language.code,
              page,
              pageSize,
              idList.values,
              fallback,
              tagsToFilterBy.values.toSet,
              exactTitleMatch,
              shouldScroll,
              embedResource.values,
              embedId,
              conceptType,
              aggregatePaths.values
            )
          }
      }

    def postSearchConcepts: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Show all concepts")
      .description("Shows all concepts. You can search it too.")
      .in("search")
      .in(jsonBody[ConceptSearchParamsDTO])
      .out(jsonBody[ConceptSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .errorOut(errorOutputsFor(400, 403, 404))
      .serverLogicPure { searchParams =>
        val lang = searchParams.language.getOrElse(LanguageCode(props.DefaultLanguage))
        scrollSearchOr(searchParams.scrollId, lang) {
          val query           = searchParams.query
          val sort            = searchParams.sort
          val language        = searchParams.language.getOrElse(LanguageCode(Language.AllLanguages))
          val pageSize        = searchParams.pageSize.getOrElse(props.DefaultPageSize)
          val page            = searchParams.page.getOrElse(1)
          val idList          = searchParams.ids
          val fallback        = searchParams.fallback.getOrElse(false)
          val tagsToFilterBy  = searchParams.tags
          val exactTitleMatch = searchParams.exactMatch.getOrElse(false)
          val shouldScroll    = searchParams.scrollId.exists(props.InitialScrollContextKeywords.contains)
          val embedResource   = searchParams.embedResource
          val embedId         = searchParams.embedId
          val conceptType     = searchParams.conceptType
          val aggregatePaths  = searchParams.aggregatePaths

          search(
            query,
            sort,
            language.code,
            page,
            pageSize,
            idList.getOrElse(List.empty),
            fallback,
            tagsToFilterBy.getOrElse(Set.empty),
            exactTitleMatch,
            shouldScroll,
            embedResource.getOrElse(List.empty),
            embedId,
            conceptType,
            aggregatePaths.getOrElse(List.empty)
          )
        }
      }

    def getTags: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Returns a list of all tags in the specified subjects")
      .description("Returns a list of all tags in the specified subjects")
      .in("tags")
      .in(language)
      .in(fallback)
      .out(
        statusCode(StatusCode.Ok).and(jsonBody[List[String]])
      )
      .errorOut(errorOutputsFor(400, 403, 404))
      .serverLogicPure { case (language, fallback) =>
        readService.allTagsFromConcepts(language.code, fallback).asRight
      }
  }
}

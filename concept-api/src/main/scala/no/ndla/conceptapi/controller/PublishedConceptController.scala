/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import cats.implicits.catsSyntaxEitherId
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.implicits.*
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
  this: WriteService
    with ReadService
    with PublishedConceptSearchService
    with SearchConverterService
    with Props
    with ConceptControllerHelpers
    with ErrorHelpers
    with TapirController =>
  val publishedConceptController: PublishedConceptController

  class PublishedConceptController extends TapirController {
    import ConceptControllerHelpers._
    import props._

    override val serviceName: String         = "concepts"
    override val prefix: EndpointInput[Unit] = "concept-api" / "v1" / serviceName

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getSubjects,
      getTags,
      getConceptById,
      getAllConcepts,
      postSearchConcepts
    )

    private def scrollSearchOr(scrollId: Option[String], language: String)(
        orFunction: => Try[(ConceptSearchResult, DynamicHeaders)]
    ): Try[(ConceptSearchResult, DynamicHeaders)] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          publishedConceptSearchService.scroll(scroll, language) match {
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
        subjects: Set[String],
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
        subjects = subjects,
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
      .out(jsonBody[Concept])
      .errorOut(errorOutputsFor(404))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (conceptId, language, fallback) =>
          readService.publishedConceptWithId(conceptId, language, fallback, user)
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
      .in(subjects)
      .in(tagsToFilterBy)
      .in(exactTitleMatch)
      .in(embedResource)
      .in(embedId)
      .in(conceptType)
      .in(aggregatePaths)
      .out(jsonBody[ConceptSearchResult])
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
              subjects,
              tagsToFilterBy,
              exactTitleMatch,
              embedResource,
              embedId,
              conceptType,
              aggregatePaths
            ) =>
          scrollSearchOr(scrollId, language) {
            val sort         = Sort.valueOf(sortStr)
            val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)

            search(
              query,
              sort,
              language,
              page,
              pageSize,
              idList.values,
              fallback,
              subjects.values.toSet,
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
      .in(jsonBody[ConceptSearchParams])
      .out(jsonBody[ConceptSearchResult])
      .out(EndpointOutput.derived[DynamicHeaders])
      .errorOut(errorOutputsFor(400, 403, 404))
      .serverLogicPure { searchParams =>
        scrollSearchOr(searchParams.scrollId, searchParams.language.getOrElse(DefaultLanguage)) {
          val query           = searchParams.query
          val sort            = searchParams.sort
          val language        = searchParams.language.getOrElse(Language.AllLanguages)
          val pageSize        = searchParams.pageSize.getOrElse(DefaultPageSize)
          val page            = searchParams.page.getOrElse(1)
          val idList          = searchParams.ids
          val fallback        = searchParams.fallback.getOrElse(false)
          val subjects        = searchParams.subjects
          val tagsToFilterBy  = searchParams.tags
          val exactTitleMatch = searchParams.exactMatch.getOrElse(false)
          val shouldScroll    = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
          val embedResource   = searchParams.embedResource
          val embedId         = searchParams.embedId
          val conceptType     = searchParams.conceptType
          val aggregatePaths  = searchParams.aggregatePaths

          search(
            query,
            sort,
            language,
            page,
            pageSize,
            idList.getOrElse(List.empty),
            fallback,
            subjects.getOrElse(Set.empty),
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

    def getSubjects: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Returns a list of all subjects used in concepts")
      .description("Returns a list of all subjects used in concepts")
      .in("subjects")
      .out(jsonBody[Set[String]])
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { _ =>
        readService.allSubjects()
      }

    def getTags: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Returns a list of all tags in the specified subjects")
      .description("Returns a list of all tags in the specified subjects")
      .in("tags")
      .in(language)
      .in(fallback)
      .in(subjects)
      .out(
        oneOf[TagOutput](
          oneOfVariant[SomeTagList](
            statusCode(StatusCode.Ok).and(jsonBody[List[SubjectTags]]).map(x => SomeTagList(x))(x => x.list)
          ),
          oneOfDefaultVariant[SomeStringList](
            statusCode(StatusCode.Ok).and(jsonBody[List[String]]).map(x => SomeStringList(x))(x => x.list)
          )
        )
      )
      .errorOut(errorOutputsFor(400, 403, 404))
      .serverLogicPure { case (language, fallback, subjects) =>
        if (subjects.values.nonEmpty) {
          publishedConceptSearchService.getTagsWithSubjects(subjects.values, language, fallback) match {
            case Success(res) => SomeTagList(res).asRight
            case Failure(ex)  => returnLeftError(ex)
          }
        } else { SomeStringList(readService.allTagsFromConcepts(language, fallback)).asRight }
      }
  }
}

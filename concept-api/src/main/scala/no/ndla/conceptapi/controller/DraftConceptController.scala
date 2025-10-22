/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.controller

import cats.implicits.*
import no.ndla.common.implicits.*
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.model.api.LanguageCode
import no.ndla.common.model.domain.concept.ConceptStatus
import no.ndla.conceptapi.model.api.*
import no.ndla.conceptapi.model.domain.Sort
import no.ndla.conceptapi.model.search.DraftSearchSettings
import no.ndla.conceptapi.service.search.{DraftConceptSearchService, SearchConverterService}
import no.ndla.conceptapi.service.{ReadService, StateTransitionRules, WriteService}
import no.ndla.conceptapi.Props
import no.ndla.language.Language.AllLanguages
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.CONCEPT_API_WRITE
import no.ndla.network.tapir.{DynamicHeaders, ErrorHandling, ErrorHelpers, TapirController}
import sttp.model.headers.CacheDirective
import sttp.model.{HeaderNames, StatusCode}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

class DraftConceptController(using
    writeService: WriteService,
    readService: ReadService,
    draftConceptSearchService: DraftConceptSearchService,
    searchConverterService: SearchConverterService,
    stateTransitionRules: StateTransitionRules,
    props: Props,
    conceptControllerHelpers: ConceptControllerHelpers,
    errorHandling: ErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: MyNDLAApiClient,
) extends TapirController {
  import conceptControllerHelpers.*
  override val serviceName: String         = "drafts"
  override val prefix: EndpointInput[Unit] = "concept-api" / "v1" / serviceName

  private val pathStatus   = path[String]("STATUS").description("Concept status")
  private val statusFilter = listQuery[String]("status").description(s"""List of statuses to filter by.
         |A draft only needs to have one of the available statuses to appear in result (OR).
       """.stripMargin)

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    getStatusStateMachine,
    getTags,
    postSearchConcepts,
    deleteLanguage,
    updateConceptStatus,
    getTagsPaginated,
    postNewConcept,
    updateConceptById,
    getConceptById,
    getAllConcepts,
  )

  private def scrollSearchOr(scrollId: Option[String], language: LanguageCode)(
      orFunction: => Try[(ConceptSearchResultDTO, DynamicHeaders)]
  ): Try[(ConceptSearchResultDTO, DynamicHeaders)] = scrollId match {
    case Some(scroll) if !props.InitialScrollContextKeywords.contains(scroll) =>
      draftConceptSearchService.scroll(scroll, language.code) match {
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
      statusFilter: Set[String],
      userFilter: Seq[String],
      shouldScroll: Boolean,
      embedResource: List[String],
      embedId: Option[String],
      responsibleId: List[String],
      conceptType: Option[String],
      aggregatePaths: List[String],
  ) = {
    val settings = DraftSearchSettings(
      withIdIn = idList,
      searchLanguage = language,
      page = page,
      pageSize = pageSize,
      sort = sort.getOrElse(Sort.ByRelevanceDesc),
      fallback = fallback,
      tagsToFilterBy = tagsToFilterBy,
      statusFilter = statusFilter,
      userFilter = userFilter,
      shouldScroll = shouldScroll,
      embedResource = embedResource,
      embedId = embedId,
      responsibleIdFilter = responsibleId,
      conceptType = conceptType,
      aggregatePaths = aggregatePaths,
    )

    val result = query.emptySomeToNone match {
      case Some(q) =>
        draftConceptSearchService.matchingQuery(q, settings.copy(sort = sort.getOrElse(Sort.ByRelevanceDesc)))
      case None => draftConceptSearchService.all(settings.copy(sort = sort.getOrElse(Sort.ByTitleDesc)))
    }

    result match {
      case Success(searchResult) =>
        val scrollHeader = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
        val output       = searchConverterService.asApiConceptSearchResult(searchResult)
        Success((output, scrollHeader))
      case Failure(ex) => Failure(ex)
    }
  }

  def getConceptById: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Show concept with a specified id")
    .description("Shows the concept for the specified id.")
    .in(pathConceptId)
    .in(language)
    .in(fallback)
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .out(jsonBody[ConceptDTO])
    .errorOut(errorOutputsFor(404))
    .withOptionalUser
    .serverLogicPure { user =>
      { case (conceptId, language, fallback) =>
        readService.conceptWithId(conceptId, language.code, fallback, user)
      }
    }

  def getAllConcepts: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Show all concepts")
    .description("Shows all concepts. You can search it too.")
    .out(jsonBody[ConceptSearchResultDTO])
    .out(EndpointOutput.derived[DynamicHeaders])
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400))
    .in(queryParam)
    .in(conceptIds)
    .in(language)
    .in(pageNo)
    .in(pageSize)
    .in(sort)
    .in(fallback)
    .in(scrollId)
    .in(tagsToFilterBy)
    .in(statusFilter)
    .in(userFilter)
    .in(embedResource)
    .in(embedId)
    .in(responsibleIdFilter)
    .in(conceptType)
    .in(aggregatePaths)
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
            statusesToFilterBy,
            usersToFilterBy,
            embedResource,
            embedId,
            responsibleIds,
            conceptType,
            aggregatePaths,
          ) => scrollSearchOr(scrollId, language) {
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
            statusesToFilterBy.values.toSet,
            usersToFilterBy.values,
            shouldScroll,
            embedResource.values,
            embedId,
            responsibleIds.values,
            conceptType,
            aggregatePaths.values,
          )
        }
    }

  def getTags: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Returns a list of all tags in the specified subjects")
    .description("Returns a list of all tags in the specified subjects")
    .in("tags")
    .in(language)
    .in(fallback)
    .out(statusCode(StatusCode.Ok).and(jsonBody[List[String]]))
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 403, 404))
    .serverLogicPure { case (language, fallback) =>
      readService.allTagsFromDraftConcepts(language.code, fallback).asRight
    }

  def postSearchConcepts: ServerEndpoint[Any, Eff] = endpoint
    .post
    .in("search")
    .summary("Show all concepts")
    .description("Shows all concepts. You can search it too.")
    .in(jsonBody[DraftConceptSearchParamsDTO])
    .out(jsonBody[ConceptSearchResultDTO])
    .out(EndpointOutput.derived[DynamicHeaders])
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 403, 404))
    .serverLogicPure { searchParams =>
      val scrollId = searchParams.scrollId
      val lang     = searchParams.language.getOrElse(LanguageCode(props.DefaultLanguage))

      scrollSearchOr(scrollId, lang) {
        val query          = searchParams.query
        val sort           = searchParams.sort
        val language       = searchParams.language.getOrElse(LanguageCode(AllLanguages))
        val pageSize       = searchParams.pageSize.getOrElse(props.DefaultPageSize)
        val page           = searchParams.page.getOrElse(1)
        val idList         = searchParams.ids
        val fallback       = searchParams.fallback.getOrElse(false)
        val tagsToFilterBy = searchParams.tags
        val statusFilter   = searchParams.status
        val userFilter     = searchParams.users
        val shouldScroll   = searchParams.scrollId.exists(props.InitialScrollContextKeywords.contains)
        val embedResource  = searchParams.embedResource.getOrElse(List.empty)
        val embedId        = searchParams.embedId
        val responsibleId  = searchParams.responsibleIds
        val conceptType    = searchParams.conceptType
        val aggregatePaths = searchParams.aggregatePaths

        search(
          query,
          sort,
          language.code,
          page,
          pageSize,
          idList.getOrElse(List.empty),
          fallback,
          tagsToFilterBy.getOrElse(Set.empty),
          statusFilter.getOrElse(Set.empty),
          userFilter.getOrElse(Seq.empty),
          shouldScroll,
          embedResource,
          embedId,
          responsibleId.getOrElse(List.empty),
          conceptType,
          aggregatePaths.getOrElse(List.empty),
        )
      }
    }

  def deleteLanguage: ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Delete language from concept")
    .description("Delete language from concept")
    .in(pathConceptId)
    .in(language)
    .out(jsonBody[ConceptDTO])
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 403, 404))
    .requirePermission(CONCEPT_API_WRITE)
    .serverLogicPure { user =>
      { case (conceptId, language) =>
        writeService.deleteLanguage(conceptId, language.code, user)
      }
    }

  def updateConceptStatus: ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Update status of a concept")
    .description("Update status of a concept")
    .in(pathConceptId / "status" / pathStatus)
    .out(jsonBody[ConceptDTO])
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .requirePermission(CONCEPT_API_WRITE)
    .serverLogicPure { user =>
      { case (conceptId, status) =>
        ConceptStatus.valueOfOrError(status).flatMap(writeService.updateConceptStatus(_, conceptId, user))
      }
    }

  def getStatusStateMachine: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Get status state machine")
    .description("Get status state machine")
    .in("status-state-machine")
    .out(jsonBody[Map[String, List[String]]])
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .requirePermission(CONCEPT_API_WRITE)
    .serverLogicPure { user => _ =>
      stateTransitionRules.stateTransitionsToApi(user).asRight
    }

  def getTagsPaginated: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Retrieves a list of all previously used tags in concepts")
    .description("Retrieves a list of all previously used tags in concepts")
    .in("tag-search")
    .in(queryParam)
    .in(pageSize)
    .in(pageNo)
    .in(language)
    .out(jsonBody[TagsSearchResultDTO])
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 403, 404))
    .serverLogicPure { case (query, pageSize, pageNo, language) =>
      val q = query.getOrElse("")
      readService.getAllTags(q, pageSize, pageNo, language.code).asRight
    }

  def postNewConcept: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Create new concept")
    .description("Create new concept")
    .in(jsonBody[NewConceptDTO])
    .out(statusCode(StatusCode.Created).and(jsonBody[ConceptDTO]))
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 403, 404))
    .requirePermission(CONCEPT_API_WRITE)
    .serverLogicPure { user =>
      { concept =>
        writeService.newConcept(concept, user)
      }
    }

  def updateConceptById: ServerEndpoint[Any, Eff] = endpoint
    .patch
    .summary("Update a concept")
    .description("Update a concept")
    .in(pathConceptId)
    .in(jsonBody[UpdatedConceptDTO])
    .out(jsonBody[ConceptDTO])
    .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
    .errorOut(errorOutputsFor(400, 403, 404))
    .requirePermission(CONCEPT_API_WRITE)
    .serverLogicPure { user =>
      { case (conceptId, updatedConcept) =>
        writeService.updateConcept(conceptId, updatedConcept, user)
      }
    }
}

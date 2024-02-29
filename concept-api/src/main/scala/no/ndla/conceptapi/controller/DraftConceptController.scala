/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import cats.implicits._
import no.ndla.common.implicits._
import no.ndla.conceptapi.model.api._
import no.ndla.conceptapi.model.domain.{ConceptStatus, Sort}
import no.ndla.conceptapi.model.search.DraftSearchSettings
import no.ndla.conceptapi.service.search.{DraftConceptSearchService, SearchConverterService}
import no.ndla.conceptapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.conceptapi.{Eff, Props}
import no.ndla.language.Language.AllLanguages
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.CONCEPT_API_WRITE
import no.ndla.network.tapir.{DynamicHeaders, Service}
import sttp.model.headers.CacheDirective
import sttp.model.{HeaderNames, StatusCode}
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.model.{CommaSeparated, Delimited}
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait DraftConceptController {
  this: WriteService
    with ReadService
    with DraftConceptSearchService
    with SearchConverterService
    with ConverterService
    with Props
    with ConceptControllerHelpers
    with ErrorHelpers =>
  val draftConceptController: DraftConceptController

  class DraftConceptController extends Service[Eff] {
    import props._

    override val serviceName: String         = "drafts"
    override val prefix: EndpointInput[Unit] = "concept-api" / "v1" / serviceName

    private val pathStatus = path[String]("STATUS").description("Concept status")
    private val statusFilter = query[CommaSeparated[String]]("status")
      .description(
        s"""List of statuses to filter by.
         |A draft only needs to have one of the available statuses to appear in result (OR).
       """.stripMargin
      )
      .default(Delimited[",", String](List.empty))

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getStatusStateMachine,
      getSubjects,
      getTags,
      postSearchConcepts,
      deleteLanguage,
      updateConceptStatus,
      getTagsPaginated,
      postNewConcept,
      updateConceptById,
      getConceptById,
      getAllConcepts
    )

    private def scrollSearchOr(scrollId: Option[String], language: String)(
        orFunction: => Try[(ConceptSearchResult, DynamicHeaders)]
    ): Try[(ConceptSearchResult, DynamicHeaders)] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          draftConceptSearchService.scroll(scroll, language) match {
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
        statusFilter: Set[String],
        userFilter: Seq[String],
        shouldScroll: Boolean,
        embedResource: Option[String],
        embedId: Option[String],
        responsibleId: List[String],
        conceptType: Option[String],
        aggregatePaths: List[String]
    ): Try[(ConceptSearchResult, DynamicHeaders)] = {
      val settings = DraftSearchSettings(
        withIdIn = idList,
        searchLanguage = language,
        page = page,
        pageSize = pageSize,
        sort = sort.getOrElse(Sort.ByRelevanceDesc),
        fallback = fallback,
        subjects = subjects,
        tagsToFilterBy = tagsToFilterBy,
        statusFilter = statusFilter,
        userFilter = userFilter,
        shouldScroll = shouldScroll,
        embedResource = embedResource,
        embedId = embedId,
        responsibleIdFilter = responsibleId,
        conceptType = conceptType,
        aggregatePaths = aggregatePaths
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
    import ConceptControllerHelpers._
    import ErrorHelpers._

    def getConceptById: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show concept with a specified id")
      .description("Shows the concept for the specified id.")
      .in(pathConceptId)
      .in(language)
      .in(fallback)
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .out(jsonBody[Concept])
      .errorOut(errorOutputsFor(404))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (conceptId, language, fallback) =>
          readService.conceptWithId(conceptId, language, fallback, user).handleErrorsOrOk
        }
      }

    def getAllConcepts: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show all concepts")
      .description("Shows all concepts. You can search it too.")
      .out(jsonBody[ConceptSearchResult])
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
      .in(subjects)
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
              subjects,
              tagsToFilterBy,
              statusesToFilterBy,
              usersToFilterBy,
              embedResource,
              embedId,
              responsibleIds,
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
              statusesToFilterBy.values.toSet,
              usersToFilterBy.values,
              shouldScroll,
              embedResource,
              embedId,
              responsibleIds.values,
              conceptType,
              aggregatePaths.values
            )
          }.handleErrorsOrOk
      }

    def getSubjects: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Returns a list of all subjects used in concepts")
      .description("Returns a list of all subjects used in concepts")
      .in("subjects")
      .out(jsonBody[Set[String]])
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { _ =>
        {
          readService.allSubjects(true).handleErrorsOrOk
        }
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
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 403, 404))
      .serverLogicPure { case (language, fallback, subjects) =>
        if (subjects.values.nonEmpty) {
          draftConceptSearchService.getTagsWithSubjects(subjects.values, language, fallback) match {
            case Success(res) if res.nonEmpty => SomeTagList(res).asRight
            case Success(_)  => returnLeftError(NotFoundException("Could not find any tags in the specified subjects"))
            case Failure(ex) => returnLeftError(ex)
          }
        } else { SomeStringList(readService.allTagsFromDraftConcepts(language, fallback)).asRight }
      }

    def postSearchConcepts: ServerEndpoint[Any, Eff] = endpoint.post
      .in("search")
      .summary("Show all concepts")
      .description("Shows all concepts. You can search it too.")
      .in(jsonBody[DraftConceptSearchParams])
      .out(jsonBody[ConceptSearchResult])
      .out(EndpointOutput.derived[DynamicHeaders])
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 403, 404))
      .serverLogicPure { searchParams =>
        val scrollId = searchParams.scrollId
        val lang     = searchParams.language

        scrollSearchOr(scrollId, lang.getOrElse(DefaultLanguage)) {
          val query          = searchParams.query
          val sort           = searchParams.sort.flatMap(Sort.valueOf)
          val language       = searchParams.language.getOrElse(AllLanguages)
          val pageSize       = searchParams.pageSize.getOrElse(DefaultPageSize)
          val page           = searchParams.page.getOrElse(1)
          val idList         = searchParams.ids
          val fallback       = searchParams.fallback.getOrElse(false)
          val subjects       = searchParams.subjects
          val tagsToFilterBy = searchParams.tags
          val statusFilter   = searchParams.status
          val userFilter     = searchParams.users
          val shouldScroll   = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
          val embedResource  = searchParams.embedResource
          val embedId        = searchParams.embedId
          val responsibleId  = searchParams.responsibleIds
          val conceptType    = searchParams.conceptType
          val aggregatePaths = searchParams.aggregatePaths

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
            statusFilter.getOrElse(Set.empty),
            userFilter.getOrElse(Seq.empty),
            shouldScroll,
            embedResource,
            embedId,
            responsibleId.getOrElse(List.empty),
            conceptType,
            aggregatePaths.getOrElse(List.empty)
          )
        }.handleErrorsOrOk
      }

    def deleteLanguage: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete language from concept")
      .description("Delete language from concept")
      .in(pathConceptId)
      .in(language)
      .out(jsonBody[Concept])
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 403, 404))
      .requirePermission(CONCEPT_API_WRITE)
      .serverLogicPure { user =>
        { case (conceptId, language) =>
          writeService.deleteLanguage(conceptId, language, user).handleErrorsOrOk
        }
      }

    def updateConceptStatus: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Update status of a concept")
      .description("Update status of a concept")
      .in(pathConceptId / "status" / pathStatus)
      .out(jsonBody[Concept])
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission(CONCEPT_API_WRITE)
      .serverLogicPure { user =>
        { case (conceptId, status) =>
          ConceptStatus
            .valueOfOrError(status)
            .flatMap(writeService.updateConceptStatus(_, conceptId, user))
            .handleErrorsOrOk
        }
      }

    def getStatusStateMachine: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get status state machine")
      .description("Get status state machine")
      .in("status-state-machine")
      .out(jsonBody[Map[String, List[String]]])
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission(CONCEPT_API_WRITE)
      .serverLogicPure { user => _ =>
        converterService.stateTransitionsToApi(user).asRight
      }

    def getTagsPaginated: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Retrieves a list of all previously used tags in concepts")
      .description("Retrieves a list of all previously used tags in concepts")
      .in("tag-search")
      .in(queryParam)
      .in(pageSize)
      .in(pageNo)
      .in(language)
      .out(jsonBody[TagsSearchResult])
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 403, 404))
      .serverLogicPure { case (query, pageSize, pageNo, language) =>
        val q = query.getOrElse("")
        readService.getAllTags(q, pageSize, pageNo, language).asRight
      }

    def postNewConcept: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Create new concept")
      .description("Create new concept")
      .in(jsonBody[NewConcept])
      .out(statusCode(StatusCode.Created).and(jsonBody[Concept]))
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 403, 404))
      .requirePermission(CONCEPT_API_WRITE)
      .serverLogicPure {
        user =>
          { concept =>
            writeService.newConcept(concept, user).handleErrorsOrOk
          }
      }

    def updateConceptById: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update a concept")
      .description("Update a concept")
      .in(pathConceptId)
      .in(jsonBody[UpdatedConcept])
      .out(jsonBody[Concept])
      .out(header(HeaderNames.CacheControl, CacheDirective.Private.toString))
      .errorOut(errorOutputsFor(400, 403, 404))
      .requirePermission(CONCEPT_API_WRITE)
      .serverLogicPure { user =>
        { case (conceptId, updatedConcept) =>
          writeService.updateConcept(conceptId, updatedConcept, user).handleErrorsOrOk
        }
      }
  }
}

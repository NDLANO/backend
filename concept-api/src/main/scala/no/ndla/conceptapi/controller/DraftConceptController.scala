/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.implicits._
import no.ndla.common.model.NDLADate
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api._
import no.ndla.conceptapi.model.domain.{ConceptStatus, SearchResult, Sort}
import no.ndla.conceptapi.model.search.DraftSearchSettings
import no.ndla.conceptapi.service.search.{DraftConceptSearchService, SearchConverterService}
import no.ndla.conceptapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.language.Language
import no.ndla.language.Language.AllLanguages
import no.ndla.network.scalatra.NdlaSwaggerSupport
import no.ndla.network.tapir.auth.Permission.CONCEPT_API_WRITE
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Created, Ok}
import org.scalatra.swagger.Swagger

import scala.util.{Failure, Success}

trait DraftConceptController {
  this: WriteService
    with ReadService
    with DraftConceptSearchService
    with SearchConverterService
    with ConverterService
    with Props
    with NdlaController
    with NdlaSwaggerSupport =>
  val draftConceptController: DraftConceptController

  class DraftConceptController(implicit val swagger: Swagger)
      extends NdlaController
      with NdlaSwaggerSupport
      with StrictLogging {
    import props._
    protected implicit override val jsonFormats: Formats =
      DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer
    val applicationDescription = "This is the Api for concept drafts"

    after() {
      // We don't want to cache draft responses in nginx since they could require access
      response.addHeader("Cache-control", "private")
    }

    private val statuss = Param[String]("STATUS", "Concept status")
    private val statusFilter = Param[Option[Seq[String]]](
      "status",
      s"""List of statuses to filter by.
         |A draft only needs to have one of the available statuses to appear in result (OR).
       """.stripMargin
    )

    private def scrollSearchOr(scrollId: Option[String], language: String)(orFunction: => Any): Any =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          draftConceptSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              Ok(searchConverterService.asApiConceptSearchResult(scrollResult), getResponseScrollHeader(scrollResult))
            case Failure(ex) => errorHandler(ex)
          }
        case _ => orFunction
      }

    private def getResponseScrollHeader(result: SearchResult[_]) =
      result.scrollId.map(i => this.scrollId.paramName -> i).toMap

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
        conceptType: Option[String]
    ) = {
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
        conceptType = conceptType
      )

      val result = query.emptySomeToNone match {
        case Some(q) =>
          draftConceptSearchService.matchingQuery(q, settings.copy(sort = sort.getOrElse(Sort.ByRelevanceDesc)))
        case None => draftConceptSearchService.all(settings.copy(sort = sort.getOrElse(Sort.ByTitleDesc)))
      }

      result match {
        case Success(searchResult) =>
          Ok(searchConverterService.asApiConceptSearchResult(searchResult), getResponseScrollHeader(searchResult))
        case Failure(ex) => errorHandler(ex)
      }

    }

    get(
      "/:concept_id",
      operation(
        apiOperation[Concept]("getConceptById")
          .summary("Show concept with a specified id")
          .description("Shows the concept for the specified id.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(language),
            asPathParam(conceptId),
            asQueryParam(fallback)
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      val conceptId = long(this.conceptId.paramName)
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, false)

      readService.conceptWithId(conceptId, language, fallback) match {
        case Success(concept) => Ok(concept)
        case Failure(ex)      => errorHandler(ex)
      }
    }: Unit

    get(
      "/",
      operation(
        apiOperation[ConceptSearchResult]("getAllConcepts")
          .summary("Show all concepts")
          .description("Shows all concepts. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(conceptIds),
            asQueryParam(language),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(sort),
            asQueryParam(fallback),
            asQueryParam(scrollId),
            asQueryParam(subjects),
            asQueryParam(tagsToFilterBy),
            asQueryParam(statusFilter),
            asQueryParam(userFilter),
            asQueryParam(embedResource),
            asQueryParam(embedId),
            asQueryParam(responsibleIdFilter),
            asQueryParam(conceptType)
          )
          .authorizations("oauth2")
          .responseMessages(response500)
      )
    ) {
      val language = paramOrDefault(this.language.paramName, AllLanguages)
      val scrollId = paramOrNone(this.scrollId.paramName)

      scrollSearchOr(scrollId, language) {
        val query              = paramOrNone(this.query.paramName)
        val sort               = paramOrNone(this.sort.paramName).flatMap(Sort.valueOf)
        val pageSize           = intOrDefault(this.pageSize.paramName, DefaultPageSize)
        val page               = intOrDefault(this.pageNo.paramName, 1)
        val idList             = paramAsListOfLong(this.conceptIds.paramName)
        val fallback           = booleanOrDefault(this.fallback.paramName, default = false)
        val subjects           = paramAsListOfString(this.subjects.paramName)
        val tagsToFilterBy     = paramAsListOfString(this.tagsToFilterBy.paramName)
        val statusesToFilterBy = paramAsListOfString(this.statusFilter.paramName)
        val usersToFilterBy    = paramAsListOfString(this.userFilter.paramName)
        val shouldScroll       = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)
        val embedResource      = paramOrNone(this.embedResource.paramName)
        val embedId            = paramOrNone(this.embedId.paramName)
        val responsibleIds     = paramAsListOfString(this.responsibleIdFilter.paramName)
        val conceptType        = paramOrNone(this.conceptType.paramName)

        search(
          query,
          sort,
          language,
          page,
          pageSize,
          idList,
          fallback,
          subjects.toSet,
          tagsToFilterBy.toSet,
          statusesToFilterBy.toSet,
          usersToFilterBy,
          shouldScroll,
          embedResource,
          embedId,
          responsibleIds,
          conceptType
        )

      }
    }: Unit

    get(
      "/subjects/",
      operation(
        apiOperation[List[String]]("getSubjects")
          .summary("Returns a list of all subjects used in concepts")
          .description("Returns a list of all subjects used in concepts")
          .parameters(
            asHeaderParam(correlationId)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      readService.allSubjects(true) match {
        case Success(subjects) => Ok(subjects)
        case Failure(ex)       => errorHandler(ex)
      }
    }: Unit

    get(
      "/tags/",
      operation(
        apiOperation[List[SubjectTags]]("getTags")
          .summary("Returns a list of all tags in the specified subjects")
          .description("Returns a list of all tags in the specified subjects")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(language),
            asQueryParam(fallback),
            asQueryParam(subjects)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      val subjects = paramAsListOfString(this.subjects.paramName)
      val language = paramOrDefault(this.language.paramName, AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      if (subjects.nonEmpty) {
        draftConceptSearchService.getTagsWithSubjects(subjects, language, fallback) match {
          case Success(res) if res.nonEmpty => Ok(res)
          case Success(_)  => errorHandler(NotFoundException("Could not find any tags in the specified subjects"))
          case Failure(ex) => errorHandler(ex)
        }
      } else {
        readService.allTagsFromDraftConcepts(language, fallback)
      }
    }: Unit

    post(
      "/search/",
      operation(
        apiOperation[ConceptSearchResult]("searchConcepts")
          .summary("Show all concepts")
          .description("Shows all concepts. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[DraftConceptSearchParams]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response500)
      )
    ) {
      val body     = tryExtract[DraftConceptSearchParams](request.body)
      val scrollId = body.map(_.scrollId).getOrElse(None)
      val lang     = body.map(_.language).toOption.flatten

      scrollSearchOr(scrollId, lang.getOrElse(DefaultLanguage)) {
        body match {
          case Success(searchParams) =>
            val query          = searchParams.query
            val sort           = searchParams.sort.flatMap(Sort.valueOf)
            val language       = searchParams.language.getOrElse(AllLanguages)
            val pageSize       = searchParams.pageSize.getOrElse(DefaultPageSize)
            val page           = searchParams.page.getOrElse(1)
            val idList         = searchParams.idList
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

            search(
              query,
              sort,
              language,
              page,
              pageSize,
              idList,
              fallback,
              subjects,
              tagsToFilterBy,
              statusFilter,
              userFilter,
              shouldScroll,
              embedResource,
              embedId,
              responsibleId,
              conceptType
            )
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    delete(
      "/:concept_id",
      operation(
        apiOperation[Concept]("deleteLanguage")
          .summary("Delete language from concept")
          .description("Delete language from concept")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(conceptId),
            asQueryParam(pathLanguage)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      val language = paramOrNone(this.language.paramName)
      requirePermissionOrAccessDeniedWithUser(CONCEPT_API_WRITE) { userInfo =>
        val id = long(this.conceptId.paramName)
        language match {
          case Some(language) => writeService.deleteLanguage(id, language, userInfo)
          case None           => Failure(NotFoundException("Language not found"))
        }
      }
    }: Unit

    put(
      "/:concept_id/status/:STATUS",
      operation(
        apiOperation[Concept]("updateConceptStatus")
          .summary("Update status of a concept")
          .description("Update status of a concept")
          .parameters(
            asPathParam(conceptId),
            asPathParam(statuss)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(CONCEPT_API_WRITE) { userInfo =>
        val id = long(this.conceptId.paramName)

        ConceptStatus
          .valueOfOrError(params(this.statuss.paramName))
          .flatMap(writeService.updateConceptStatus(_, id, userInfo)) match {
          case Success(concept) => concept
          case Failure(ex)      => errorHandler(ex)
        }

      }
    }: Unit

    get(
      "/status-state-machine/",
      operation(
        apiOperation[Map[String, List[String]]]("getStatusStateMachine")
          .summary("Get status state machine")
          .description("Get status state machine")
          .authorizations("oauth2")
          .responseMessages(response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(CONCEPT_API_WRITE) { user =>
        converterService.stateTransitionsToApi(user)
      }
    }: Unit

    get(
      "/tag-search/",
      operation(
        apiOperation[TagsSearchResult]("getTags-paginated")
          .summary("Retrieves a list of all previously used tags in concepts")
          .description("Retrieves a list of all previously used tags in concepts")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo),
            asQueryParam(language)
          )
          .responseMessages(response500)
          .authorizations("oauth2")
      )
    ) {

      val query = paramOrDefault(this.query.paramName, "")
      val pageSize = intOrDefault(this.pageSize.paramName, props.DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => props.DefaultPageSize
        case x                        => x
      }
      val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }
      val language = paramOrDefault(this.language.paramName, AllLanguages)

      readService.getAllTags(query, pageSize, pageNo, language)
    }: Unit

    post(
      "/",
      operation(
        apiOperation[Concept]("newConceptById")
          .summary("Create new concept")
          .description("Create new concept")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[NewConcept]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(CONCEPT_API_WRITE) { userInfo =>
        val body = tryExtract[NewConcept](request.body)
        body.flatMap(concept => writeService.newConcept(concept, userInfo)) match {
          case Success(c)  => Created(c)
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    patch(
      "/:concept_id",
      operation(
        apiOperation[Concept]("updateConceptById")
          .summary("Update a concept")
          .description("Update a concept")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[UpdatedConcept],
            asPathParam(conceptId)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(CONCEPT_API_WRITE) { userInfo =>
        val body      = tryExtract[UpdatedConcept](request.body)
        val conceptId = long(this.conceptId.paramName)
        body.flatMap(writeService.updateConcept(conceptId, _, userInfo)) match {
          case Success(c)  => Ok(c)
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit
  }
}

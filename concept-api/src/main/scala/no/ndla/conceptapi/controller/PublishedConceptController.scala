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
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.conceptapi.model.search.SearchSettings
import no.ndla.conceptapi.service.search.{PublishedConceptSearchService, SearchConverterService}
import no.ndla.conceptapi.service.{ReadService, WriteService}
import no.ndla.language.Language
import no.ndla.network.scalatra.NdlaSwaggerSupport
import no.ndla.network.tapir.auth.TokenUser
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok
import org.scalatra.swagger.Swagger

import scala.util.{Failure, Success}

trait PublishedConceptController {
  this: WriteService
    with ReadService
    with PublishedConceptSearchService
    with SearchConverterService
    with Props
    with NdlaController
    with NdlaSwaggerSupport =>
  val publishedConceptController: PublishedConceptController

  class PublishedConceptController(implicit val swagger: Swagger)
      extends NdlaController
      with NdlaSwaggerSupport
      with StrictLogging {
    import props._
    protected implicit override val jsonFormats: Formats =
      DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer
    val applicationDescription = "This is the Api for concepts"

    private def scrollSearchOr(scrollId: Option[String], language: String)(orFunction: => Any): Any =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          publishedConceptSearchService.scroll(scroll, language) match {
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
        exactTitleMatch: Boolean,
        shouldScroll: Boolean,
        embedResource: Option[String],
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
      val user     = TokenUser.fromScalatraRequest(request).toOption

      readService.publishedConceptWithId(conceptId, language, fallback, user) match {
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
            asQueryParam(exactTitleMatch),
            asQueryParam(embedResource),
            asQueryParam(embedId),
            asQueryParam(conceptType),
            asQueryParam(aggregatePaths)
          )
          authorizations "oauth2"
          responseMessages response500
      )
    ) {
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val scrollId = paramOrNone(this.scrollId.paramName)

      scrollSearchOr(scrollId, language) {
        val query           = paramOrNone(this.query.paramName)
        val sort            = paramOrNone(this.sort.paramName).flatMap(Sort.valueOf)
        val pageSize        = intOrDefault(this.pageSize.paramName, DefaultPageSize)
        val page            = intOrDefault(this.pageNo.paramName, 1)
        val idList          = paramAsListOfLong(this.conceptIds.paramName)
        val fallback        = booleanOrDefault(this.fallback.paramName, default = false)
        val subjects        = paramAsListOfString(this.subjects.paramName)
        val tagsToFilterBy  = paramAsListOfString(this.tagsToFilterBy.paramName)
        val exactTitleMatch = booleanOrDefault(this.exactTitleMatch.paramName, default = false)
        val shouldScroll    = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)
        val embedResource   = paramOrNone(this.embedResource.paramName)
        val embedId         = paramOrNone(this.embedId.paramName)
        val conceptType     = paramOrNone(this.conceptType.paramName)
        val aggregatePaths  = paramAsListOfString(this.aggregatePaths.paramName)

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
          exactTitleMatch,
          shouldScroll,
          embedResource,
          embedId,
          conceptType,
          aggregatePaths
        )

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
            bodyParam[ConceptSearchParams]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response500)
      )
    ) {
      val body     = tryExtract[ConceptSearchParams](request.body)
      val scrollId = body.map(_.scrollId).getOrElse(None)
      val lang     = body.map(_.language).toOption.flatten

      scrollSearchOr(scrollId, lang.getOrElse(DefaultLanguage)) {
        body match {
          case Success(searchParams) =>
            val query           = searchParams.query
            val sort            = searchParams.sort.flatMap(Sort.valueOf)
            val language        = searchParams.language.getOrElse(Language.AllLanguages)
            val pageSize        = searchParams.pageSize.getOrElse(DefaultPageSize)
            val page            = searchParams.page.getOrElse(1)
            val idList          = searchParams.idList
            val fallback        = searchParams.fallback.getOrElse(false)
            val subjects        = searchParams.subjects
            val tagsToFilterBy  = searchParams.tags
            val exactTitleMatch = searchParams.exactTitleMatch.getOrElse(false)
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
              idList,
              fallback,
              subjects,
              tagsToFilterBy,
              exactTitleMatch,
              shouldScroll,
              embedResource,
              embedId,
              conceptType,
              aggregatePaths
            )
          case Failure(ex) => errorHandler(ex)
        }
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
      readService.allSubjects() match {
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
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      if (subjects.nonEmpty) {
        publishedConceptSearchService.getTagsWithSubjects(subjects, language, fallback) match {
          case Success(res) if res.nonEmpty => Ok(res)
          case Success(_)  => errorHandler(NotFoundException("Could not find any tags in the specified subjects"))
          case Failure(ex) => errorHandler(ex)
        }
      } else {
        readService.allTagsFromConcepts(language, fallback)
      }
    }: Unit

  }
}

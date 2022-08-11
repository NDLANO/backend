/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.model.api
import no.ndla.audioapi.model.api.{
  AudioMetaInformation,
  Error,
  NewAudioMetaInformation,
  Series,
  SeriesSearchParams,
  SeriesSummarySearchResult,
  UpdatedAudioMetaInformation,
  ValidationError
}
import no.ndla.audioapi.model.domain.SeriesSearchSettings
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.service.search.{SearchConverterService, SeriesSearchService}
import no.ndla.audioapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.language.Language
import no.ndla.scalatra.NdlaSwaggerSupport
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger._

import scala.util.{Failure, Success, Try}

trait SeriesController {
  this: ReadService
    with WriteService
    with SeriesSearchService
    with Role
    with User
    with SearchConverterService
    with ConverterService
    with Props
    with NdlaController =>
  val seriesController: SeriesController

  class SeriesController(implicit val swagger: Swagger)
      extends NdlaController
      with FileUploadSupport
      with NdlaSwaggerSupport {
    import props._
    protected implicit override val jsonFormats: Formats = DefaultFormats ++ JavaTimeSerializers.all
    protected val applicationDescription                 = "Services for accessing audio."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()
    registerModel[NewAudioMetaInformation]()
    registerModel[UpdatedAudioMetaInformation]()

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxAudioFileSizeBytes)))

    val response400: ResponseMessage = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403: ResponseMessage = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404: ResponseMessage = ResponseMessage(404, "Not found", Some("Error"))
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    private val seriesId = Param[String]("series_id", "Id of series.")
    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only results with titles or tags matching the specified query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]](
      "page-size",
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
    )
    private val pathLanguage = Param[String]("language", "The ISO 639-1 language code describing language.")

    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
         |""".stripMargin
    )
    private val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")

    /** Does a scroll with [[SeriesSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String)(orFunction: => Any): Any =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          seriesSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiSeriesSummarySearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case _ => orFunction
      }

    get(
      "/",
      operation(
        apiOperation[SeriesSummarySearchResult]("getSeries")
          .summary("Find series")
          .description("Shows all the series. Also searchable.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(language),
            asQueryParam(sort),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(scrollId),
            asQueryParam(fallback)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val language = paramOrNone("language")
      val scrollId = paramOrNone(this.scrollId.paramName)

      scrollSearchOr(scrollId, language.getOrElse(Language.AllLanguages)) {
        val query        = paramOrNone(this.query.paramName)
        val sort         = paramOrNone(this.sort.paramName)
        val pageSize     = paramOrNone(this.pageSize.paramName).flatMap(ps => Try(ps.toInt).toOption)
        val page         = paramOrNone(this.pageNo.paramName).flatMap(idx => Try(idx.toInt).toOption)
        val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)
        val fallback     = booleanOrDefault(this.fallback.paramName, default = false)

        search(query, language, sort, pageSize, page, shouldScroll, fallback)
      }
    }

    post(
      "/search/",
      operation(
        apiOperation[List[SeriesSummarySearchResult]]("getSeriesPost")
          .summary("Find series")
          .description("Shows all the series. Also searchable.")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[SeriesSearchParams],
            asQueryParam(scrollId)
          )
          .responseMessages(response400, response500)
      )
    ) {
      val searchParams = extract[SeriesSearchParams](request.body)
      scrollSearchOr(searchParams.scrollId, searchParams.language.getOrElse(Language.AllLanguages)) {
        val query        = searchParams.query
        val language     = searchParams.language
        val sort         = searchParams.sort
        val pageSize     = searchParams.pageSize
        val page         = searchParams.page
        val shouldScroll = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
        val fallback     = searchParams.fallback.getOrElse(false)

        search(query, language, sort, pageSize, page, shouldScroll, fallback)
      }
    }

    private def search(
        query: Option[String],
        language: Option[String],
        sort: Option[String],
        pageSize: Option[Int],
        page: Option[Int],
        shouldScroll: Boolean,
        fallback: Boolean
    ) = {
      val searchSettings = query match {
        case Some(q) =>
          SeriesSearchSettings(
            query = Some(q),
            language = language,
            page = page,
            pageSize = pageSize,
            sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
            shouldScroll = shouldScroll,
            fallback = fallback
          )

        case None =>
          SeriesSearchSettings(
            query = None,
            language = language,
            page = page,
            pageSize = pageSize,
            sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
            shouldScroll = shouldScroll,
            fallback = fallback
          )
      }

      seriesSearchService.matchingQuery(searchSettings) match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiSeriesSummarySearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/:series_id",
      operation(
        apiOperation[AudioMetaInformation]("findBySeriesId")
          .summary("Fetch information for series")
          .description("Shows info of the series with submitted id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(this.seriesId),
            asQueryParam(language)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val id       = long(this.seriesId.paramName)
      val language = paramOrNone(this.language.paramName)

      readService.seriesWithId(id, language) match {
        case Success(series) => Ok(series)
        case Failure(ex)     => errorHandler(ex)
      }
    }

    delete(
      "/:series_id",
      operation(
        apiOperation[Unit]("deleteSeries")
          .summary("Deletes series with the specified id")
          .description("Deletes series with the specified id")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(seriesId)
          )
          .responseMessages(response403, response404, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val seriesId = long(this.seriesId.paramName)
      writeService.deleteSeries(seriesId) match {
        case Failure(ex) => errorHandler(ex)
        case Success(_)  => NoContent()
      }
    }

    delete(
      "/:series_id/language/:language",
      operation(
        apiOperation[Series]("deleteLanguage")
          .summary("Delete language version of audio metadata.")
          .description("Delete language version of audio metadata.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(seriesId),
            asPathParam(pathLanguage)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val seriesId = long(this.seriesId.paramName)
      val language = params(this.pathLanguage.paramName)

      writeService.deleteSeriesLanguageVersion(seriesId, language) match {
        case Failure(ex)          => errorHandler(ex)
        case Success(Some(image)) => Ok(image)
        case Success(None)        => NoContent()
      }
    }

    post(
      "/",
      operation(
        apiOperation[api.Series]("newSeries")
          .summary("Create a new series with meta information")
          .description("Create a new series with meta information")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[api.NewSeries]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val newSeries = extract[api.NewSeries](request.body)

      writeService.newSeries(newSeries) match {
        case Success(s)  => Created(s)
        case Failure(ex) => errorHandler(ex)
      }
    }

    put(
      "/:series_id",
      operation(
        apiOperation[api.Series]("updateSeries")
          .summary("Upload audio for a different language or update metadata for an existing audio-file")
          .description("Update the metadata for an existing language, or upload metadata for a new language.")
          .consumes("multipart/form-data")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(seriesId),
            bodyParam[api.NewSeries]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)
      val id           = long(this.seriesId.paramName)
      val updateSeries = extract[api.NewSeries](request.body)

      writeService.updateSeries(id, updateSeries) match {
        case Success(series) => Ok(series)
        case Failure(e)      => errorHandler(e)
      }

    }
  }
}

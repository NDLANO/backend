/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import cats.implicits._
import io.circe.generic.auto._
import no.ndla.audioapi.{Eff, Props}
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain.SeriesSearchSettings
import no.ndla.audioapi.service.search.{SearchConverterService, SeriesSearchService}
import no.ndla.audioapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.language.Language
import no.ndla.network.tapir.NoNullJsonPrinter._
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.AUDIO_API_WRITE
import no.ndla.common.implicits._
import sttp.model.StatusCode
import sttp.tapir.EndpointIO.annotations.{header, jsonbody}
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._

import scala.util.{Failure, Success, Try}

trait SeriesController {
  this: ReadService
    with WriteService
    with SeriesSearchService
    with SearchConverterService
    with ConverterService
    with Props
    with ErrorHelpers =>
  val seriesController: SeriesController
  class SeriesController() extends Service[Eff] {

    import ErrorHelpers._
    import props._

    private val queryString = query[Option[String]]("query")
      .description("Return only results with titles or tags matching the specified query.")
    private val language =
      query[Option[String]]("language").description("The ISO 639-1 language code describing language.")
    private val pageNo = query[Option[Int]]("page").description("The page number of the search hits to display.")
    private val pageSize = query[Option[Int]]("page-size").description(
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
    )
    private val sort = query[Option[String]]("sort").description(
      s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val scrollId = query[Option[String]]("search-context").description(
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.name}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.name}' and '${this.pageSize.name}' instead.
         |""".stripMargin
    )
    private val fallback =
      query[Option[Boolean]]("fallback").description("Fallback to existing language if language is specified.")
    private val pathSeriesId = path[Long]("series-id").description("Id of series.")
    private val pathLanguage = path[String]("language").description("The ISO 639-1 language code describing language.")

    override val serviceName: String         = "series"
    override val prefix: EndpointInput[Unit] = "audio-api" / "v1" / serviceName

    def getSeriesSearch: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Find series")
      .description("Shows all the series. Also searchable.")
      .out(EndpointOutput.derived[SummaryWithHeader])
      .in(queryString)
      .in(language)
      .in(sort)
      .in(pageNo)
      .in(pageSize)
      .in(scrollId)
      .in(fallback)
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { case (query, language, sort, page, pageSize, scrollId, fallback) =>
        scrollSearchOr(scrollId, language.getOrElse(Language.AllLanguages)) {
          val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)
          search(query, language, Sort.valueOf(sort), pageSize, page, shouldScroll, fallback.getOrElse(false))
        }.handleErrorsOrOk
      }

    def postSeriesSearch: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Find series")
      .description("Shows all the series. Also searchable.")
      .in("search")
      .in(jsonBody[SeriesSearchParams])
      .out(EndpointOutput.derived[SummaryWithHeader])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { searchParams =>
        scrollSearchOr(searchParams.scrollId, searchParams.language.getOrElse(Language.AllLanguages)) {
          val query        = searchParams.query
          val language     = searchParams.language
          val sort         = searchParams.sort
          val pageSize     = searchParams.pageSize
          val page         = searchParams.page
          val shouldScroll = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
          val fallback     = searchParams.fallback.getOrElse(false)

          search(query, language, sort, pageSize, page, shouldScroll, fallback)
        }.handleErrorsOrOk
      }

    def getSingleSeries: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch information for series")
      .description("Shows info of the series with submitted id.")
      .in(pathSeriesId)
      .in(language)
      .errorOut(errorOutputsFor(400, 404))
      .out(jsonBody[Series])
      .serverLogicPure { case (id, language) =>
        readService.seriesWithId(id, language).handleErrorsOrOk
      }

    def deleteSeries: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Deletes series with the specified id")
      .description("Deletes series with the specified id")
      .in(pathSeriesId)
      .out(emptyOutput)
      .errorOut(errorOutputsFor(400, 403, 404))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ => seriesId =>
        writeService.deleteSeries(seriesId) match {
          case Failure(ex) => returnLeftError(ex)
          case Success(_)  => Right(())
        }
      }

    def deleteLanguage: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete language version of audio metadata.")
      .description("Delete language version of audio metadata.")
      .in(pathSeriesId)
      .in("language")
      .in(pathLanguage)
      .out(noContentOrBodyOutput[Series])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ => input =>
        val (seriesId, language) = input
        writeService.deleteSeriesLanguageVersion(seriesId, language) match {
          case Failure(ex)           => returnLeftError(ex)
          case Success(Some(series)) => Some(series).asRight
          case Success(None)         => None.asRight
        }
      }

    def postNewSeries: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Create a new series with meta information")
      .description("Create a new series with meta information")
      .in(jsonBody[NewSeries])
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(statusCode(StatusCode.Created).and(jsonBody[Series]))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ => newSeries =>
        writeService
          .newSeries(newSeries)
          .handleErrorsOrOk
      }

    def putUpdateSeries: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Upload audio for a different language or update metadata for an existing audio-file")
      .description("Update the metadata for an existing language, or upload metadata for a new language.")
      .in(pathSeriesId)
      .in(jsonBody[NewSeries])
      .out(jsonBody[Series])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ => input =>
        val (id, updateSeries) = input
        writeService.updateSeries(id, updateSeries).handleErrorsOrOk
      }

    private case class SummaryWithHeader(
        @jsonbody
        body: SeriesSummarySearchResult,
        @header("search-context")
        searchContext: Option[String]
    )

    private def search(
        query: Option[String],
        language: Option[String],
        sort: Option[Sort],
        pageSize: Option[Int],
        page: Option[Int],
        shouldScroll: Boolean,
        fallback: Boolean
    ): Try[SummaryWithHeader] = {
      val searchSettings = query.emptySomeToNone match {
        case Some(q) =>
          SeriesSearchSettings(
            query = Some(q),
            language = language,
            page = page,
            pageSize = pageSize,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            shouldScroll = shouldScroll,
            fallback = fallback
          )

        case None =>
          SeriesSearchSettings(
            query = None,
            language = language,
            page = page,
            pageSize = pageSize,
            sort = sort.getOrElse(Sort.ByTitleAsc),
            shouldScroll = shouldScroll,
            fallback = fallback
          )
      }

      seriesSearchService.matchingQuery(searchSettings).map { searchResult =>
        SummaryWithHeader(
          body = searchConverterService.asApiSeriesSummarySearchResult(searchResult),
          searchContext = searchResult.scrollId
        )
      }
    }

    /** Does a scroll with [[SeriesSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result
      */
    private def scrollSearchOr(scrollId: Option[String], language: String)(
        orFunction: => Try[SummaryWithHeader]
    ): Try[SummaryWithHeader] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          seriesSearchService.scroll(scroll, language).map { scrollResult =>
            SummaryWithHeader(
              body = searchConverterService.asApiSeriesSummarySearchResult(scrollResult),
              searchContext = scrollResult.scrollId
            )
          }
        case _ => orFunction
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getSeriesSearch,
      postSeriesSearch,
      getSingleSeries,
      deleteSeries,
      deleteLanguage,
      postNewSeries,
      putUpdateSeries
    )

  }
}

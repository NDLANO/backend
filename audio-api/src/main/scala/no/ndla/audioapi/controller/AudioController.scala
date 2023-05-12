/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser._
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain.{AudioType, SearchSettings}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{AudioSearchService, SearchConverterService}
import no.ndla.audioapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.language.Language
import no.ndla.network.scalatra.NdlaSwaggerSupport
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Scope.AUDIO_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import sttp.model.Part
import sttp.tapir.EndpointIO.annotations.{header, jsonbody}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{EndpointInput, _}

import scala.util.{Failure, Success, Try}

trait AudioController {
  this: AudioRepository
    with ReadService
    with WriteService
    with AudioSearchService
    with SearchConverterService
    with ConverterService
    with Props
    with ErrorHelpers
    with NdlaSwaggerSupport
    with Service =>
  val audioApiController: AudioController

  class AudioController() extends SwaggerService {
    import props._
    override val prefix: EndpointInput[Unit] = "audio-api" / "v1" / "audio"

    private val queryString = query[Option[String]]("query")
      .description("Return only results with titles or tags matching the specified query.")
    private val language =
      query[Option[String]]("language").description("The ISO 639-1 language code describing language.")
    private val license = query[Option[String]]("license").description("Return only audio with provided license.")
    private val pageNo  = query[Option[Int]]("page").description("The page number of the search hits to display.")
    private val pageSize = query[Option[Int]]("page-size").description(
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
    )
    private val audioIds = query[List[Long]]("ids").description(
      "Return only audios that have one of the provided ids. To provide multiple ids, separate by comma (,)."
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
    private val audioType = query[Option[String]]("audio-type").description(
      s"""Only return types of the specified value.
         |Possible values are ${AudioType.all.mkString("'", ", ", "'")}""".stripMargin
    )
    private val seriesFilter = query[Option[Boolean]]("filter-by-series").description(
      """Filter result by whether they are a part of a series or not.
        |'true' will return only audios that are a part of a series.
        |'false' will return only audios that are NOT a part of a series.
        |Not specifying will return both audios that are a part of a series and not.""".stripMargin
    )
    private val fallback =
      query[Option[Boolean]]("fallback").description("Fallback to existing language if language is specified.")
    private val pathAudioId  = path[Long]("audio-id").description("Id of audio.")
    private val pathLanguage = path[String]("language").description("The ISO 639-1 language code describing language.")

    import ErrorHelpers._

    val index: ServerEndpoint[Any, IO] = endpoint.get
      .summary("Find audio files")
      .description("Shows all the audio files in the ndla.no database. You can search it too.")
      .out(EndpointOutput.derived[SummaryWithHeader])
      .in(queryString)
      .in(language)
      .in(license)
      .in(sort)
      .in(pageNo)
      .in(pageSize)
      .in(scrollId)
      .in(audioType)
      .in(seriesFilter)
      .in(fallback)
      .errorOut(errorOutputsFor(400, 404)) // TODO: Figure out which codes we need :^)
      .securityIn(auth.bearer[Option[TokenUser]]())
      .serverSecurityLogicPure(requireScope(AUDIO_API_WRITE))
      .serverLogicPure { _ => input =>
        val (query, language, license, sort, pageNo, pageSize, scrollId, audioType, seriesFilter, fallback) = input
        scrollSearchOr(scrollId, language.getOrElse(Language.AllLanguages)) {
          val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)
          search(
            query,
            language,
            license,
            sort,
            pageSize,
            pageNo,
            shouldScroll,
            audioType,
            seriesFilter,
            fallback.getOrElse(false)
          )
        }.handleErrorsOrOk
      }

    val postSearch: ServerEndpoint[Any, IO] = endpoint.post
      .summary("Find audio files")
      .description("Shows all the audio files in the ndla.no database. You can search it too.")
      .in("search")
      .in(jsonBody[SearchParams])
      .out(jsonBody[SummaryWithHeader])
      .errorOut(errorOutputsFor(400, 404)) // TODO: Figure out which codes we need :^)
      .serverLogicPure { searchParams =>
        scrollSearchOr(searchParams.scrollId, searchParams.language.getOrElse(Language.AllLanguages)) {
          val shouldScroll = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
          search(
            searchParams.query,
            searchParams.language,
            searchParams.license,
            searchParams.sort,
            searchParams.pageSize,
            searchParams.page,
            shouldScroll,
            searchParams.audioType,
            searchParams.filterBySeries,
            searchParams.fallback.getOrElse(false)
          )
        }.handleErrorsOrOk
      }

    val getSingle: ServerEndpoint[Any, IO] = endpoint.get
      .summary("Fetch information for audio file")
      .description("Shows info of the audio with submitted id.")
      .in(pathAudioId)
      .in(language)
      .errorOut(errorOutputsFor(400, 404))
      .out(jsonBody[AudioMetaInformation])
      .serverLogicPure { case (id, language) =>
        readService.withId(id, language) match {
          case Some(audio) => audio.asRight
          case None        => notFoundWithMsg(s"Audio with id $id not found").asLeft
        }
      }

    val getIds: ServerEndpoint[Any, IO] = endpoint.get
      .in("ids")
      .in(audioIds)
      .in(language)
      .errorOut(errorOutputsFor(400, 404)) // TODO: What codes?
      .out(jsonBody[List[AudioMetaInformation]])
      .summary("Fetch audio that matches ids parameter.")
      .description("Fetch audios that matches ids parameter.")
      .serverLogicPure { case (audioIds, language) =>
        readService.getAudiosByIds(audioIds, language).handleErrorsOrOk
      }

    val deleteAudio: ServerEndpoint[Any, IO] = endpoint.delete
      .summary("Deletes audio with the specified id")
      .description("Deletes audio with the specified id")
      .in(pathAudioId)
      .errorOut(errorOutputsFor(400, 401, 403, 404)) // TODO: what codes?
      .out(emptyOutput)
      .securityIn(auth.bearer[Option[TokenUser]]())
      .serverSecurityLogicPure(requireScope(AUDIO_API_WRITE))
      .serverLogicPure { _ => audioId =>
        writeService.deleteAudioAndFiles(audioId) match {
          case Failure(ex) => returnError(ex).asLeft
          case Success(_)  => Right(())
        }
      }

    val deleteLanguage: ServerEndpoint[Any, IO] = endpoint.delete
      .summary("Delete language version of audio metadata.")
      .description("Delete language version of audio metadata.")
      .in(pathAudioId)
      .in("language")
      .in(pathLanguage)
      .out(jsonBody[Option[AudioMetaInformation]])
      .prependOut(emptyOutput)
      .errorOut(errorOutputsFor(400, 401, 403, 404)) // TODO: What codes?
      .securityIn(auth.bearer[Option[TokenUser]]())
      .serverSecurityLogicPure(requireScope(AUDIO_API_WRITE))
      .serverLogicPure { _ => input =>
        val (audioId, language) = input
        writeService.deleteAudioLanguageVersion(audioId, language) match {
          // TODO: Test that this returns NoContent (204) on Success(none)
          case Success(Some(audio)) => Right(Some(audio))
          case Success(None)        => Right(None)
          case Failure(ex)          => returnError(ex).asLeft
        }
      }

    val postNewAudio: ServerEndpoint[Any, IO] = endpoint.post
      .summary("Upload a new audio file with meta information")
      .description("Upload a new audio file with meta data")
      .in(multipartBody[MetaDataAndFileForm])
      .out(jsonBody[AudioMetaInformation])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .securityIn(auth.bearer[Option[TokenUser]]())
      .serverSecurityLogicPure(requireScope(AUDIO_API_WRITE))
      .serverLogicPure { user => formData =>
        parse(formData.metadata).flatMap(_.as[NewAudioMetaInformation]) match {
          case Right(metaInformation) =>
            writeService.storeNewAudio(metaInformation, formData.file, user) match {
              case Success(audioMeta) => Right(audioMeta)
              case Failure(e)         => returnError(e).asLeft
            }
          case _ =>
            badRequest("Could not deserialize `metadata` form field as `NewAudioMetaInformation` json.").asLeft
        }
      }

    val putUpdateAudio: ServerEndpoint[Any, IO] = endpoint.put
      .summary("Upload audio for a different language or update metadata for an existing audio-file")
      .description("Update the metadata for an existing language, or upload metadata for a new language.")
      .in(pathAudioId)
      .in(multipartBody[MetaDataAndOptFileForm])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[AudioMetaInformation])
      .securityIn(auth.bearer[Option[TokenUser]]())
      .serverSecurityLogicPure(requireScope(AUDIO_API_WRITE))
      .serverLogicPure { user => input =>
        val (id, body) = input
        parse(body.metadata).flatMap(_.as[UpdatedAudioMetaInformation]) match {
          case Right(metaInformation) =>
            writeService.updateAudio(id, metaInformation, body.file, user) match {
              case Success(audioMeta) => audioMeta.asRight
              case Failure(e)         => returnError(e).asLeft
            }
          case _ =>
            badRequest("Could not deserialize `metadata` form field as `UpdatedAudioMetaInformation` json.").asLeft
        }
      }

    val tagSearch: ServerEndpoint[Any, IO] = endpoint.get
      .summary("Retrieves a list of all previously used tags in audios")
      .description("Retrieves a list of all previously used tags in audios")
      .in("/tag-search/")
      .in(queryString)
      .in(pageSize)
      .in(pageNo)
      .in(language)
      .out(jsonBody[TagsSearchResult])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { case (query, ps, pn, lang) =>
        val pageSize = ps.getOrElse(DefaultPageSize) match {
          case tooSmall if tooSmall < 1 => DefaultPageSize
          case x                        => x
        }
        val pageNo = pn.getOrElse(1) match {
          case tooSmall if tooSmall < 1 => 1
          case x                        => x
        }

        val language = lang.getOrElse(Language.AllLanguages)

        readService.getAllTags(query.getOrElse(""), pageSize, pageNo, language) match {
          case Failure(ex)     => returnError(ex).asLeft
          case Success(result) => result.asRight
        }
      }

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      index,
      postSearch,
      getSingle,
      getIds,
      deleteAudio,
      deleteLanguage,
      postNewAudio,
      putUpdateAudio,
      tagSearch
    )

    case class MetaDataAndFileForm(metadata: String, file: Part[Array[Byte]])
    case class MetaDataAndOptFileForm(metadata: String, file: Option[Part[Array[Byte]]])

    case class SummaryWithHeader(
        @jsonbody
        body: AudioSummarySearchResult,
        @header("search-context")
        searchContext: Option[String]
    )

    /** Does a scroll with [[AudioSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String)(
        orFunction: => Try[SummaryWithHeader]
    ): Try[SummaryWithHeader] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          audioSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val body = searchConverterService.asApiAudioSummarySearchResult(scrollResult)
              Success(SummaryWithHeader(body = body, searchContext = scrollResult.scrollId))
            case Failure(ex) => Failure(ex)
          }
        case _ => orFunction
      }

    private def search(
        query: Option[String],
        language: Option[String],
        license: Option[String],
        sort: Option[String],
        pageSize: Option[Int],
        page: Option[Int],
        shouldScroll: Boolean,
        audioType: Option[String],
        seriesFilter: Option[Boolean],
        fallback: Boolean
    ): Try[SummaryWithHeader] = {
      val searchSettings = query match {
        case Some(q) =>
          SearchSettings(
            query = Some(q),
            language = language,
            license = license,
            page = page,
            pageSize = pageSize,
            sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
            shouldScroll = shouldScroll,
            audioType = audioType.flatMap(AudioType.valueOf),
            seriesFilter = seriesFilter,
            fallback = fallback
          )

        case None =>
          SearchSettings(
            query = None,
            language = language,
            license = license,
            page = page,
            pageSize = pageSize,
            sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
            shouldScroll = shouldScroll,
            audioType = audioType.flatMap(AudioType.valueOf),
            seriesFilter = seriesFilter,
            fallback = fallback
          )
      }

      audioSearchService.matchingQuery(searchSettings) match {
        case Success(searchResult) =>
          Success(
            SummaryWithHeader(
              body = searchConverterService.asApiAudioSummarySearchResult(searchResult),
              searchContext = searchResult.scrollId
            )
          )
        case Failure(ex) => Failure(ex)
      }
    }
  }
}

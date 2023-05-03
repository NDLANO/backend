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
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.api.{
  AudioMetaInformation,
  AudioSummarySearchResult,
  ErrorHelpers,
  NewAudioMetaInformation,
  SearchParams,
  TagsSearchResult,
  UpdatedAudioMetaInformation,
  ValidationError
}
import no.ndla.audioapi.model.domain.{AudioType, SearchSettings}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{AudioSearchService, SearchConverterService}
import no.ndla.audioapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.language.Language
import no.ndla.network.scalatra.NdlaSwaggerSupport
import no.ndla.network.tapir.Service
import scala.util.{Failure, Success, Try}
import sttp.tapir.EndpointInput
import cats.effect.IO
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.tapir.Schema
import sttp.tapir.generic._
import sttp.tapir.generic.auto._
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._
import java.io.File
import sttp.model.Part
import sttp.tapir.generic.auto._

trait AudioController {
  this: AudioRepository
    with ReadService
    with WriteService
    with AudioSearchService
    with Role
    with User
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

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .summary("Find audio files")
        .description("Shows all the audio files in the ndla.no database. You can search it too.")
        .out(jsonBody[AudioSummarySearchResult])
        // .out(header[Option[String]]("search-context")) // TODO: Hvordan får man headere til å fungere i output?
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
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: Figure out which codes we need :^)
        .serverLogicPure {
          case (query, language, license, sort, pageNo, pageSize, scrollId, audioType, seriesFilter, fallback) =>
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
        },
      endpoint.post
        .summary("Find audio files")
        .description("Shows all the audio files in the ndla.no database. You can search it too.")
        .in("/search/")
        .in(jsonBody[SearchParams])
        .out(jsonBody[AudioSummarySearchResult])
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: Figure out which codes we need :^)
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
        },
      endpoint.get
        .summary("Fetch information for audio file")
        .description("Shows info of the audio with submitted id.")
        .in(pathAudioId)
        .in(language)
        .errorOut(errorOutputsFor(400, 403, 404))
        .out(jsonBody[AudioMetaInformation])
        .serverLogicPure { case (id, language) =>
          readService.withId(id, language) match {
            case Some(audio) => audio.asRight
            case None        => notFoundWithMsg(s"Audio with id $id not found").asLeft
          }
        },
      endpoint.get
        .in(audioIds)
        .in(language)
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: What codes?
        .out(jsonBody[List[AudioMetaInformation]])
        .summary("Fetch audio that matches ids parameter.")
        .description("Fetch audios that matches ids parameter.")
        .serverLogicPure { case (audioIds, language) =>
          readService.getAudiosByIds(audioIds, language).handleErrorsOrOk
        },
      endpoint.delete
        .summary("Deletes audio with the specified id")
        .description("Deletes audio with the specified id")
        .in(pathAudioId)
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: what codes?
        .out(emptyOutput)
        .serverLogicPure { audioId =>
          // TODO: Consider rewriting user assertions to .serverSecurityLogicPure or something like that
          authUser.assertHasId()
          authRole.assertHasRole(RoleWithWriteAccess)

          writeService.deleteAudioAndFiles(audioId) match {
            case Failure(ex) => returnError(ex).asLeft
            case Success(_)  => Right(())
          }
        },
      endpoint.delete
        .summary("Delete language version of audio metadata.")
        .description("Delete language version of audio metadata.")
        .in(pathAudioId)
        .in("language")
        .in(pathLanguage)
        .out(jsonBody[Option[AudioMetaInformation]])
        .prependOut(emptyOutput)
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: What codes?
        .serverLogicPure { case (audioId, language) =>
          // TODO: Consider rewriting user assertions to .serverSecurityLogicPure or something like that
          authUser.assertHasId()
          authRole.assertHasRole(RoleWithWriteAccess)

          writeService.deleteAudioLanguageVersion(audioId, language) match {
            // TODO: Test that this returns NoContent (204) on success(none)
            case Success(Some(audio)) => Right(Some(audio))
            case Success(None)        => Right(None)
            case Failure(ex)          => returnError(ex).asLeft
          }

        },
      endpoint.post
        .summary("Upload a new audio file with meta information")
        .description("Upload a new audio file with meta data")
        .in(multipartBody) // TODO: Could we use `multipartBody[NewAudioForm]` somehow?
        .out(jsonBody[AudioMetaInformation])
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: what codes?
        .serverLogicPure { inForm =>
          authUser.assertHasId()
          authRole.assertHasRole(RoleWithWriteAccess)

          // TODO: No questionmarks
          writeService.storeNewAudio(???, ???) match {
            case Success(audioMeta) => Right(audioMeta)
            case Failure(e)         => returnError(e).asLeft
          }
        },
      endpoint.put
        .summary("Upload audio for a different language or update metadata for an existing audio-file")
        .description("Update the metadata for an existing language, or upload metadata for a new language.")
        .in(pathAudioId)
        .in(multipartBody)                        // TODO:
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: what codes?
        .out(jsonBody[AudioMetaInformation])
        .serverLogicPure { case (id, body) =>
          authUser.assertHasId()
          authRole.assertHasRole(RoleWithWriteAccess)

          // TODO: Get the questionmarks from the body
          writeService.updateAudio(id, ???, ???) match {
            case Success(audioMeta) => audioMeta.asRight
            case Failure(e)         => returnError(e).asLeft
          }
        },
      endpoint.get
        .summary("Retrieves a list of all previously used tags in audios")
        .description("Retrieves a list of all previously used tags in audios")
        .in("/tag-search/")
        .in(queryString)
        .in(pageSize)
        .in(pageNo)
        .in(language)
        .out(jsonBody[TagsSearchResult])
        .errorOut(errorOutputsFor(400, 403, 404)) // TODO: what codes?
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
    )

    case class NewAudioForm(metadata: Part[String], file: Part[File])

    /** Does a scroll with [[AudioSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr[T](scrollId: Option[String], language: String)(
        orFunction: => Try[AudioSummarySearchResult]
    ): Try[AudioSummarySearchResult] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          audioSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.name -> i).toMap
              // TODO: Headers
              Success(searchConverterService.asApiAudioSummarySearchResult(scrollResult))
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
    ) = {
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
          val responseHeader =
            searchResult.scrollId
              .map(i => this.scrollId.name -> i)
              .toMap // TODO: This needs to be returned somehow, probably with a case class with a response body and headers :^)
          Success(searchConverterService.asApiAudioSummarySearchResult(searchResult))
        case Failure(ex) => Failure(ex)
      }
    }
  }
}

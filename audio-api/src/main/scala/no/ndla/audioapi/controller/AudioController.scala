/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import cats.implicits.*
import io.circe.generic.auto.*
import no.ndla.audioapi.controller.multipart.{MetaDataAndFileForm, MetaDataAndOptFileForm}
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.api.*
import no.ndla.audioapi.model.domain.{AudioType, SearchSettings}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{AudioSearchService, SearchConverterService}
import no.ndla.audioapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.common.errors.FileTooBigException
import no.ndla.language.Language
import no.ndla.common.implicits.*
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.model.api.LanguageCode
import no.ndla.common.model.domain.UploadedFile
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.{NonEmptyString, TapirController}
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.AUDIO_API_WRITE
import sttp.model.Part
import sttp.tapir.EndpointIO.annotations.{header, jsonbody}
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*

import java.io.File
import scala.util.{Failure, Success, Try}

trait AudioController {
  this: AudioRepository & ReadService & WriteService & AudioSearchService & SearchConverterService & ConverterService &
    Props & ErrorHandling & TapirController =>
  val audioApiController: AudioController

  class AudioController extends TapirController {
    import props.*
    val maxAudioFileSizeBytes: Int           = props.MaxAudioFileSizeBytes
    override val serviceName: String         = "audio"
    override val prefix: EndpointInput[Unit] = "audio-api" / "v1" / serviceName

    private val queryString = query[Option[NonEmptyString]]("query")
      .description("Return only results with titles or tags matching the specified query.")
      .schema(NonEmptyString.schemaOpt)
    private val language =
      query[Option[LanguageCode]]("language")
        .description("The ISO 639-1 language code describing language.")
        .default(Some(LanguageCode(Language.AllLanguages)))
    private val license  = query[Option[String]]("license").description("Return only audio with provided license.")
    private val pageNo   = query[Option[Int]]("page").description("The page number of the search hits to display.")
    private val pageSize = query[Option[Int]]("page-size").description(
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
    )
    private val audioIds = listQuery[Long]("ids").description(
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

    import ErrorHelpers.*

    def getSearch: ServerEndpoint[Any, Eff] = endpoint.get
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
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure {
        case (query, language, license, sort, pageNo, pageSize, scrollId, audioType, seriesFilter, fallback) =>
          val lang = language.getOrElse(LanguageCode(Language.AllLanguages))
          scrollSearchOr(scrollId, lang.code) {
            val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)
            search(
              query.underlying,
              language.map(_.code),
              license,
              sort.flatMap(Sort.valueOf),
              pageSize,
              pageNo,
              shouldScroll,
              audioType,
              seriesFilter,
              fallback.getOrElse(false)
            )
          }
      }

    def postSearch: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Find audio files")
      .description("Shows all the audio files in the ndla.no database. You can search it too.")
      .in("search")
      .in(jsonBody[SearchParamsDTO])
      .out(EndpointOutput.derived[SummaryWithHeader])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { searchParams =>
        val lang = searchParams.language.getOrElse(LanguageCode(Language.AllLanguages))
        scrollSearchOr(searchParams.scrollId, lang.code) {
          val shouldScroll = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
          search(
            searchParams.query,
            searchParams.language.map(_.code),
            searchParams.license,
            searchParams.sort,
            searchParams.pageSize,
            searchParams.page,
            shouldScroll,
            searchParams.audioType,
            searchParams.filterBySeries,
            searchParams.fallback.getOrElse(false)
          )
        }
      }

    def getSingle: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch information for audio file")
      .description("Shows info of the audio with submitted id.")
      .in(pathAudioId)
      .in(language)
      .errorOut(errorOutputsFor(400, 404))
      .out(jsonBody[AudioMetaInformationDTO])
      .serverLogicPure { case (id, language) =>
        readService.withId(id, language.map(_.code)) match {
          case Some(audio) => audio.asRight
          case None        => notFoundWithMsg(s"Audio with id $id not found").asLeft
        }
      }

    def getIds: ServerEndpoint[Any, Eff] = endpoint.get
      .in("ids")
      .in(audioIds)
      .in(language)
      .errorOut(errorOutputsFor(400, 404))
      .out(jsonBody[Array[AudioMetaInformationDTO]])
      .summary("Fetch audio that matches ids parameter.")
      .description("Fetch audios that matches ids parameter.")
      .serverLogicPure { case (audioIds, language) =>
        // NOTE: For some weird reason the generated openapi spec fails if this is a List[AudioMetaInformation]
        //       I assume it is because of the recursive nature of `AudioMetaInformation`.
        //       For Array[AudioMetaInformation] it works fine so lets just convert it here for now.
        readService.getAudiosByIds(audioIds.values, language.map(_.code)).map(_.toArray)
      }

    def deleteAudio: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Deletes audio with the specified id")
      .description("Deletes audio with the specified id")
      .in(pathAudioId)
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(noContent)
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ => audioId =>
        writeService.deleteAudioAndFiles(audioId).map(_ => ())
      }

    def deleteLanguage: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete language version of audio metadata.")
      .description("Delete language version of audio metadata.")
      .in(pathAudioId)
      .in("language")
      .in(pathLanguage)
      .out(noContentOrBodyOutput[AudioMetaInformationDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { _ => input =>
        val (audioId, language) = input
        writeService.deleteAudioLanguageVersion(audioId, language) match {
          case Success(Some(audio)) => Right(Some(audio))
          case Success(None)        => Right(None)
          case Failure(ex)          => returnLeftError(ex)
        }
      }

    def postNewAudio: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Upload a new audio file with meta information")
      .description("Upload a new audio file with meta data")
      .in(multipartBody[MetaDataAndFileForm])
      .out(jsonBody[AudioMetaInformationDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 413))
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { user => formData =>
        doWithStream(formData.file) { uploadedFile =>
          writeService.storeNewAudio(
            formData.metadata.body,
            uploadedFile,
            user
          )
        }
      }

    def putUpdateAudio: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Upload audio for a different language or update metadata for an existing audio-file")
      .description("Update the metadata for an existing language, or upload metadata for a new language.")
      .in(pathAudioId)
      .in(multipartBody[MetaDataAndOptFileForm])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 413))
      .out(jsonBody[AudioMetaInformationDTO])
      .requirePermission(AUDIO_API_WRITE)
      .serverLogicPure { user => input =>
        {
          val (id, formData) = input
          formData.file match {
            case Some(f) =>
              doWithStream(f) { stream =>
                writeService.updateAudio(id, formData.metadata.body, Some(stream), user)
              }
            case None =>
              writeService.updateAudio(id, formData.metadata.body, None, user)
          }
        }
      }

    def tagSearch: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Retrieves a list of all previously used tags in audios")
      .description("Retrieves a list of all previously used tags in audios")
      .in("tag-search")
      .in(queryString)
      .in(pageSize)
      .in(pageNo)
      .in(language)
      .out(jsonBody[TagsSearchResultDTO])
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

        val language = lang.getOrElse(LanguageCode(Language.AllLanguages))

        readService.getAllTags(query.underlyingOrElse(""), pageSize, pageNo, language.code)
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getSearch,
      postSearch,
      getIds,
      tagSearch,
      getSingle,
      deleteAudio,
      deleteLanguage,
      postNewAudio,
      putUpdateAudio
    )

    def doWithStream[T](filePart: Part[File])(f: UploadedFile => Try[T]): Try[T] = {
      val file = UploadedFile.fromFilePart(filePart)
      if (file.fileSize > maxAudioFileSizeBytes) Failure(FileTooBigException())
      else file.doWithStream(f)
    }

    private case class SummaryWithHeader(
        @jsonbody
        body: AudioSummarySearchResultDTO,
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
        sort: Option[Sort],
        pageSize: Option[Int],
        page: Option[Int],
        shouldScroll: Boolean,
        audioType: Option[String],
        seriesFilter: Option[Boolean],
        fallback: Boolean
    ): Try[SummaryWithHeader] = {
      val searchSettings = query.emptySomeToNone match {
        case Some(q) =>
          SearchSettings(
            query = Some(q),
            language = language.map(Language.languageOrParam),
            license = license,
            page = page,
            pageSize = pageSize,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            shouldScroll = shouldScroll,
            audioType = audioType.flatMap(AudioType.valueOf),
            seriesFilter = seriesFilter,
            fallback = fallback
          )

        case None =>
          SearchSettings(
            query = None,
            language = language.map(Language.languageOrParam),
            license = license,
            page = page,
            pageSize = pageSize,
            sort = sort.getOrElse(Sort.ByTitleAsc),
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

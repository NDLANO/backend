/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.AudioApiProperties._
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.api.{
  AudioMetaInformation,
  AudioSummarySearchResult,
  Error,
  NewAudioMetaInformation,
  SearchParams,
  TagsSearchResult,
  UpdatedAudioMetaInformation,
  ValidationError,
  ValidationException,
  ValidationMessage
}
import no.ndla.audioapi.model.domain.{AudioType, SearchSettings}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{AudioSearchService, SearchConverterService}
import no.ndla.audioapi.service.{Clock, ConverterService, ReadService, WriteService}
import no.ndla.language.Language
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger._
import org.scalatra.util.NotNothing

import scala.util.{Failure, Success, Try}

trait AudioController {
  this: AudioRepository
    with ReadService
    with WriteService
    with AudioSearchService
    with Role
    with User
    with Clock
    with SearchConverterService
    with ConverterService =>
  val audioApiController: AudioController

  class AudioController(implicit val swagger: Swagger)
      extends NdlaController
      with FileUploadSupport
      with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats + new EnumNameSerializer(AudioType)
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

    case class Param[T](paramName: String, description: String)

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only results with titles or tags matching the specified query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license  = Param[Option[String]]("license", "Return only audio with provided license.")
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
    private val audioId      = Param[String]("audio_id", "Id of audio.")
    private val pathLanguage = Param[String]("language", "The ISO 639-1 language code describing language.")
    private val metadataNewAudio =
      Param[NewAudioMetaInformation]("metadata", "The metadata for the audio file to submit.")
    private val metadataUpdatedAudio =
      Param[UpdatedAudioMetaInformation]("metadata", "The metadata for the audio file to submit.")
    private val file = Param("file", "The audio file to upload")

    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
         |""".stripMargin
    )

    private val audioType = Param[Option[String]](
      "audio-type",
      s"""Only return types of the specified value.
         |Possible values are ${AudioType.all.mkString("'", ", ", "'")}""".stripMargin
    )

    private val seriesFilter = Param[Option[Boolean]](
      "filter-by-series",
      """Filter result by whether they are a part of a series or not.
        |'true' will return only audios that are a part of a series.
        |'false' will return only audios that are NOT a part of a series.
        |Not specifying will return both audios that are a part of a series and not.""".stripMargin
    )

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)
    private def asObjectFormParam[T: Manifest: NotNothing](param: Param[T]) = {
      val className = manifest[T].runtimeClass.getSimpleName
      val modelOpt  = models.get(className)

      modelOpt match {
        case Some(value) =>
          formParam(param.paramName, value).description(param.description)
        case None =>
          logger.error(s"${param.paramName} could not be resolved as object formParam, doing regular formParam.")
          formParam[T](param.paramName).description(param.description)
      }
    }
    private def asFileParam(param: Param[_]) =
      Parameter(
        name = param.paramName,
        `type` = ValueDataType("file"),
        description = Some(param.description),
        paramType = ParamType.Form
      )

    /** Does a scroll with [[AudioSearchService]] If no scrollId is specified execute the function @orFunction in the
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
          audioSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiAudioSummarySearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case _ => orFunction
      }

    get(
      "/",
      operation(
        apiOperation[AudioSummarySearchResult]("getAudioFiles")
          .summary("Find audio files")
          .description("Shows all the audio files in the ndla.no database. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(language),
            asQueryParam(license),
            asQueryParam(sort),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(scrollId),
            asQueryParam(audioType),
            asQueryParam(seriesFilter)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val language = paramOrNone("language")
      val scrollId = paramOrNone(this.scrollId.paramName)

      scrollSearchOr(scrollId, language.getOrElse(Language.AllLanguages)) {
        val query        = paramOrNone("query")
        val license      = paramOrNone("license")
        val sort         = paramOrNone("sort")
        val pageSize     = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
        val page         = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)
        val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)
        val atype        = paramOrNone(audioType.paramName)
        val seriesFilter = booleanOrNone(this.seriesFilter.paramName)

        search(
          query,
          language,
          license,
          sort,
          pageSize,
          page,
          shouldScroll,
          atype,
          seriesFilter
        )
      }
    }

    post(
      "/search/",
      operation(
        apiOperation[List[AudioSummarySearchResult]]("getAudioFilesPost")
          .summary("Find audio files")
          .description("Shows all the audio files in the ndla.no database. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[SearchParams],
            asQueryParam(scrollId)
          )
          .responseMessages(response400, response500)
      )
    ) {
      val searchParams = extract[SearchParams](request.body)
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
          searchParams.filterBySeries
        )
      }
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
        seriesFilter: Option[Boolean]
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
            seriesFilter = seriesFilter
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
            seriesFilter = seriesFilter
          )
      }

      audioSearchService.matchingQuery(searchSettings) match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiAudioSummarySearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/:audio_id",
      operation(
        apiOperation[AudioMetaInformation]("findByAudioId")
          .summary("Fetch information for audio file")
          .description("Shows info of the audio with submitted id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(audioId),
            asQueryParam(language)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val id       = long(this.audioId.paramName)
      val language = paramOrNone(this.language.paramName)

      readService.withId(id, language) match {
        case Some(audio) => Ok(audio)
        case None        => NotFound(Error(Error.NOT_FOUND, s"Audio with id $id not found"))
      }
    }

    delete(
      "/:audio_id",
      operation(
        apiOperation[Unit]("deleteAudio")
          .summary("Deletes audio with the specified id")
          .description("Deletes audio with the specified id")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(audioId)
          )
          .responseMessages(response403, response404, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val audioId = long(this.audioId.paramName)
      writeService.deleteAudioAndFiles(audioId) match {
        case Failure(ex) => errorHandler(ex)
        case Success(_)  => Ok()
      }
    }

    delete(
      "/:audio_id/language/:language",
      operation(
        apiOperation[AudioMetaInformation]("deleteLanguage")
          .summary("Delete language version of audio metadata.")
          .description("Delete language version of audio metadata.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(audioId),
            asPathParam(pathLanguage)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val audioId  = long(this.audioId.paramName)
      val language = params(this.pathLanguage.paramName)

      writeService.deleteAudioLanguageVersion(audioId, language) match {
        case Failure(ex)          => errorHandler(ex)
        case Success(Some(image)) => Ok(image)
        case Success(None)        => NoContent()
      }
    }

    post(
      "/",
      operation(
        apiOperation[AudioMetaInformation]("newAudio")
          .summary("Upload a new audio file with meta information")
          .description("Upload a new audio file with meta data")
          .consumes("multipart/form-data")
          .parameters(
            asHeaderParam(correlationId),
            asObjectFormParam(metadataNewAudio),
            asFileParam(file)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val newAudio = params
        .get(this.metadataNewAudio.paramName)
        .map(extract[NewAudioMetaInformation])
        .getOrElse(
          throw new ValidationException(
            errors = Seq(ValidationMessage("metadata", "The request must contain audio metadata."))
          )
        )

      fileParams.get(this.file.paramName) match {
        case Some(file) =>
          writeService.storeNewAudio(newAudio, file) match {
            case Success(audioMeta) => audioMeta
            case Failure(e)         => errorHandler(e)
          }
        case None =>
          errorHandler(
            new ValidationException(errors = Seq(ValidationMessage("file", "The request must contain one file.")))
          )
      }
    }

    put(
      "/:audio_id",
      operation(
        apiOperation[AudioMetaInformation]("updateAudio")
          .summary("Upload audio for a different language or update metadata for an existing audio-file")
          .description("Update the metadata for an existing language, or upload metadata for a new language.")
          .consumes("multipart/form-data")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(audioId),
            asObjectFormParam(metadataUpdatedAudio),
            asFileParam(file)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)
      val id      = long(this.audioId.paramName)
      val fileOpt = fileParams.get(this.file.paramName)

      val updatedAudio = params
        .get(this.metadataUpdatedAudio.paramName)
        .map(extract[UpdatedAudioMetaInformation])
        .getOrElse(
          throw new ValidationException(
            errors = Seq(ValidationMessage("metadata", "The request must contain audio metadata"))
          )
        )

      writeService.updateAudio(id, updatedAudio, fileOpt) match {
        case Success(audioMeta) => audioMeta
        case Failure(e)         => errorHandler(e)
      }

    }

    get(
      "/tag-search/",
      operation(
        apiOperation[TagsSearchResult]("getTags-paginated")
          .summary("Retrieves a list of all previously used tags in audios")
          .description("Retrieves a list of all previously used tags in audios")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo),
            asQueryParam(language)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      val query = paramOrDefault(this.query.paramName, "")
      val pageSize = intOrDefault(this.pageSize.paramName, DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => DefaultPageSize
        case x                        => x
      }
      val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      readService.getAllTags(query, pageSize, pageNo, language) match {
        case Failure(ex)     => errorHandler(ex)
        case Success(result) => Ok(result)
      }
    }
  }
}

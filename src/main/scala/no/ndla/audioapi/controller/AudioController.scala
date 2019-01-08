/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.AudioApiProperties.{
  DefaultPageSize,
  MaxPageSize,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  MaxAudioFileSizeBytes,
  RoleWithWriteAccess
}
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.model.{Language, Sort}
import no.ndla.audioapi.model.api.{
  AudioMetaInformation,
  Error,
  NewAudioMetaInformation,
  SearchParams,
  SearchResult,
  UpdatedAudioMetaInformation,
  ValidationError,
  ValidationException,
  ValidationMessage
}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{SearchConverterService, SearchService}
import no.ndla.audioapi.service.{Clock, ConverterService, ReadService, WriteService}
import org.json4s.native.Serialization.read
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
    with SearchService
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
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "Services for accessing audio."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()
    registerModel[NewAudioMetaInformation]()
    registerModel[UpdatedAudioMetaInformation]()

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxAudioFileSizeBytes)))

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    case class Param[T](paramName: String, description: String)

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only results with titles or tags matching the specified query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license = Param[Option[String]]("license", "Return only audio with provided license.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]](
      "page-size",
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize.")
    private val audioId = Param[String]("audio_id", "Id of audio.")
    private val metadataNewAudio =
      Param[NewAudioMetaInformation]("metadata", "The metadata for the audio file to submit.")
    private val metadataUpdatedAudio =
      Param[UpdatedAudioMetaInformation]("metadata", "The metadata for the audio file to submit.")
    private val file = Param("file", "The audio file to upload")

    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A search context retrieved from the response header of a previous search.
         |If search-context is specified, all other query parameters, except '${this.language.paramName}' is ignored.
         |For the rest of the parameters the original search of the search-context is used.
         |The search context may change between scrolls. Always use the most recent one (The context if unused dies after $ElasticSearchScrollKeepAlive).
         |Used to enable scrolling past $ElasticSearchIndexMaxResultWindow results.
      """.stripMargin
    )

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)
    private def asObjectFormParam[T: Manifest: NotNothing](param: Param[T]) = {
      val className = manifest[T].runtimeClass.getSimpleName
      val modelOpt = models.get(className)

      modelOpt match {
        case Some(value) =>
          formParam(param.paramName, value).description(param.description)
        case None =>
          logger.error(s"${param.paramName} could not be resolved as object formParam, doing regular formParam.")
          formParam[T](param.paramName).description(param.description)
      }
    }
    private def asFileParam(param: Param[_]) =
      Parameter(name = param.paramName,
                `type` = ValueDataType("file"),
                description = Some(param.description),
                paramType = ParamType.Form)

    /**
      * Does a scroll with [[SearchService]]
      * If no scrollId is specified execute the function @orFunction in the second parameter list.
      *
      * @param orFunction Function to execute if no scrollId in parameters (Usually searching)
      * @return A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(orFunction: => Any): Any = {
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      paramOrNone(this.scrollId.paramName) match {
        case Some(scroll) =>
          searchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiSearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case None => orFunction
      }
    }

    get(
      "/",
      operation(
        apiOperation[SearchResult]("getAudioFiles")
          summary "Find audio files"
          description "Shows all the audio files in the ndla.no database. You can search it too."
          parameters (
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(language),
            asQueryParam(license),
            asQueryParam(sort),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(scrollId)
        )
          responseMessages (response404, response500))
    ) {
      scrollSearchOr {
        val query = paramOrNone("query")
        val language = paramOrNone("language")
        val license = paramOrNone("license")
        val sort = paramOrNone("sort")
        val pageSize = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
        val page = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)

        search(query, language, license, sort, pageSize, page)
      }
    }

    post(
      "/search/",
      operation(
        apiOperation[List[SearchResult]]("getAudioFilesPost")
          summary "Find audio files"
          description "Shows all the audio files in the ndla.no database. You can search it too."
          parameters (
            asHeaderParam(correlationId),
            bodyParam[SearchParams],
            asQueryParam(scrollId)
        )
          responseMessages (response400, response500))
    ) {
      scrollSearchOr {
        val searchParams = extract[SearchParams](request.body)
        val query = searchParams.query
        val language = searchParams.language
        val license = searchParams.license
        val sort = searchParams.sort
        val pageSize = searchParams.pageSize
        val page = searchParams.page

        search(query, language, license, sort, pageSize, page)
      }
    }

    private def search(query: Option[String],
                       language: Option[String],
                       license: Option[String],
                       sort: Option[String],
                       pageSize: Option[Int],
                       page: Option[Int]) = {
      val result = query match {
        case Some(q) =>
          searchService.matchingQuery(
            query = q,
            language = language,
            license = license,
            page = page,
            pageSize = pageSize,
            sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc)
          )

        case None =>
          searchService.all(
            language = language,
            license = license,
            page = page,
            pageSize = pageSize,
            sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc)
          )
      }

      result match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiSearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/:audio_id",
      operation(
        apiOperation[AudioMetaInformation]("findByAudioId")
          summary "Fetch information for audio file"
          description "Shows info of the audio with submitted id."
          parameters (
            asHeaderParam(correlationId),
            asPathParam(audioId),
            asQueryParam(language)
        )
          responseMessages (response404, response500))
    ) {
      val id = long(this.audioId.paramName)
      val language = paramOrNone(this.language.paramName)

      readService.withId(id, language) match {
        case Some(audio) => audio
        case None        => NotFound(Error(Error.NOT_FOUND, s"Audio with id $id not found"))
      }
    }

    post(
      "/",
      operation(
        apiOperation[AudioMetaInformation]("newAudio")
          summary "Upload a new audio file with meta information"
          description "Upload a new audio file with meta data"
          consumes "multipart/form-data"
          parameters (
            asHeaderParam(correlationId),
            asObjectFormParam(metadataNewAudio),
            asFileParam(file)
        )
          authorizations "oauth2"
          responseMessages (response400, response403, response500))
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val newAudio = params
        .get(this.metadataNewAudio.paramName)
        .map(extract[NewAudioMetaInformation])
        .getOrElse(throw new ValidationException(
          errors = Seq(ValidationMessage("metadata", "The request must contain audio metadata."))))

      val file = fileParams.getOrElse(
        this.file.paramName,
        throw new ValidationException(errors = Seq(ValidationMessage("file", "The request must contain one file."))))

      writeService.storeNewAudio(newAudio, file) match {
        case Success(audioMeta) => audioMeta
        case Failure(e)         => errorHandler(e)
      }
    }

    put(
      "/:audio_id",
      operation(
        apiOperation[AudioMetaInformation]("updateAudio")
          summary "Upload audio for a different language or update metadata for an existing audio-file"
          description "Update the metadata for an existing language, or upload metadata for a new language."
          consumes "multipart/form-data"
          parameters (
            asHeaderParam(correlationId),
            asPathParam(audioId),
            asObjectFormParam(metadataUpdatedAudio),
            asFileParam(file)
        )
          authorizations "oauth2"
          responseMessages (response400, response403, response500))
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)
      val id = long(this.audioId.paramName)
      val fileOpt = fileParams.get(this.file.paramName)

      val updatedAudio = params
        .get(this.metadataUpdatedAudio.paramName)
        .map(extract[UpdatedAudioMetaInformation])
        .getOrElse(throw new ValidationException(
          errors = Seq(ValidationMessage("metadata", "The request must contain audio metadata"))))

      writeService.updateAudio(id, updatedAudio, fileOpt) match {
        case Success(audioMeta) => audioMeta
        case Failure(e)         => errorHandler(e)
      }

    }

    def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
      Try(read[T](json)) match {
        case Success(data) => data
        case Failure(e) =>
          logger.error(e.getMessage, e)
          throw new ValidationException(errors = Seq(ValidationMessage("body", e.getMessage)))
      }
    }

  }
}

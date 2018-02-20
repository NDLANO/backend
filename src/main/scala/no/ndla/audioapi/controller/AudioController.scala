/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.AudioApiProperties.{MaxAudioFileSizeBytes, RoleWithWriteAccess}
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.model.{Language, Sort}
import no.ndla.audioapi.model.api.{AudioMetaInformation, Error, NewAudioMetaInformation, SearchParams, SearchResult, UpdatedAudioMetaInformation, ValidationError, ValidationException, ValidationMessage}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.SearchService
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
  this: AudioRepository with ReadService with WriteService with SearchService with Role with User with Clock with ConverterService =>
  val audioApiController: AudioController

  class AudioController(implicit val swagger: Swagger) extends NdlaController with FileUploadSupport with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "Services for accessing audio."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()
    registerModel[NewAudioMetaInformation]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    case class Param(paramName:String, description:String)

    private val correlationId = Param("X-Correlation-ID","User supplied correlation-id. May be omitted.")
    private val query = Param("query","Return only results with titles or tags matching the specified query.")
    private val language = Param("language", "The ISO 639-1 language code describing language.")
    private val license = Param("license","Return only audio with provided license.")
    private val sort = Param("sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin)
    private val pageNo = Param("page","The page number of the search hits to display.")
    private val pageSize = Param("page-size","The number of search hits to display for each page.")
    private val audioId = Param("audio_id","Id of audio.")
    private val metadataNewAudio = Param("metadata",
      """The metadata for the audio file to submit. Format (as JSON):
            {
             title: String,
             language: String,
             copyritght: Copyright,
             tags: Array[String]
             }""".stripMargin)
    private val metadataUpdatedAudio = Param("metadata",
      """The metadata for the audio file to submit. Format (as JSON):
            {
             revision: Int,
             title: String,
             language: String,
             copyritght: Copyright,
             tags: Array[String]
             }""".stripMargin)
    private val file = Param("file", "The audio file to upload")


    private def asQueryParam[T: Manifest: NotNothing](param: Param) = queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param) = headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param) = pathParam[T](param.paramName).description(param.description)
    private def asFormParam[T: Manifest: NotNothing](param: Param) = formParam[T](param.paramName).description(param.description)
    private def asFileParam(param: Param) = Parameter(name = param.paramName, `type` = ValueDataType("file"), description = Some(param.description), paramType = ParamType.Form)


    val getAudioFiles =
      (apiOperation[SearchResult]("getAudioFiles")
        summary "Find audio files"
        notes "Shows all the audio files in the ndla.no database. You can search it too."
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[String]](license),
        asQueryParam[Option[String]](sort),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize)
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

    get("/", operation(getAudioFiles)) {
      val query = paramOrNone("query")
      val language = paramOrNone("language")
      val license = paramOrNone("license")
      val sort = paramOrNone("sort")
      val pageSize = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)

      search(query, language, license, sort, pageSize, page)
    }

    val getAudioFilesPost =
      (apiOperation[List[SearchResult]]("getAudioFilesPost")
        summary "Find audio files"
        notes "Shows all the audio files in the ndla.no database. You can search it too."
        parameters(
        asHeaderParam[Option[String]](correlationId),
        bodyParam[SearchParams]
      )
        authorizations "oauth2"
        responseMessages(response400, response500))

    post("/search/", operation(getAudioFilesPost)) {
      val searchParams = extract[SearchParams](request.body)
      val query = searchParams.query
      val language = searchParams.language
      val license = searchParams.license
      val sort = searchParams.sort
      val pageSize = searchParams.pageSize
      val page = searchParams.page

      search(query, language, license, sort, pageSize, page)
    }

    def search(query: Option[String], language: Option[String], license: Option[String], sort: Option[String], pageSize: Option[Int], page: Option[Int]) = {
      query match {
        case Some(q) => searchService.matchingQuery(
          query = q,
          language = language,
          license = license,
          page = page,
          pageSize = pageSize,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc))

        case None => searchService.all(
          language = language,
          license = license,
          page = page,
          pageSize = pageSize,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc))
      }
    }

    val getByAudioId =
      (apiOperation[AudioMetaInformation]("findByAudioId")
        summary "Fetch information for audio file"
        notes "Shows info of the audio with submitted id."
        parameters(
          asHeaderParam[Option[String]](correlationId),
          asPathParam[String](audioId),
          asQueryParam[Option[String]](language)
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

    get("/:audio_id", operation(getByAudioId)) {
      val id = long(this.audioId.paramName)
      val language = paramOrNone(this.language.paramName)

      readService.withId(id, language) match {
        case Some(audio) => audio
        case None => NotFound(Error(Error.NOT_FOUND, s"Audio with id $id not found"))
      }
    }

    val newAudio =
      (apiOperation[AudioMetaInformation]("newAudio")
        summary "Upload a new audio file with meta information"
        notes "Upload a new audio file with meta data"
        consumes "multipart/form-data"
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asFormParam[String](metadataNewAudio),
        asFileParam(file)
        )
        authorizations "oauth2"
        responseMessages(response400, response403, response500))

    post("/", operation(newAudio)) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val newAudio = params.get(this.metadataNewAudio.paramName)
        .map(extract[NewAudioMetaInformation])
        .getOrElse(throw new ValidationException(errors=Seq(ValidationMessage("metadata", "The request must contain audio metadata"))))

      val file = fileParams.getOrElse(this.file.paramName, throw new ValidationException(errors=Seq(ValidationMessage("file", "The request must contain one file"))))

      writeService.storeNewAudio(newAudio, file) match {
        case Success(audioMeta) => audioMeta
        case Failure(e) => errorHandler(e)
      }
    }

    val updateAudio =
      (apiOperation[AudioMetaInformation]("updateAudio")
        summary "Upload audio for a different language or update metadata for an existing audio-file"
        notes "Update the metadata for an existing language, or upload metadata for a new language."
        consumes "multipart/form-data"
        parameters(
          asHeaderParam[Option[String]](correlationId),
          asPathParam[String](audioId),
          asFormParam[String](metadataUpdatedAudio),
          asFileParam(file)
        )
        authorizations "oauth2"
        responseMessages(response400, response403, response500))

    put("/:audio_id", operation(updateAudio)) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)
      val id = long(this.audioId.paramName)
      val fileOpt = fileParams.get(this.file.paramName)

      val updatedAudio = params.get(this.metadataUpdatedAudio.paramName)
        .map(extract[UpdatedAudioMetaInformation])
        .getOrElse(throw new ValidationException(errors=Seq(ValidationMessage("metadata", "The request must contain audio metadata"))))

      writeService.updateAudio(id, updatedAudio, fileOpt) match {
        case Success(audioMeta) => audioMeta
        case Failure(e) => errorHandler(e)
      }

    }

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxAudioFileSizeBytes)))

    def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
      Try(read[T](json)) match {
        case Success(data) => data
        case Failure(e) =>
          logger.error(e.getMessage, e)
          throw new ValidationException(errors=Seq(ValidationMessage("body", e.getMessage)))
      }
    }

  }
}

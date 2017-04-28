/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import java.io.File

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.api.{AudioMetaInformation, Error, NewAudioMetaInformation, SearchResult, ValidationError, ValidationException, ValidationMessage}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.{AuthenticationRole, Clock, ReadService, WriteService}
import no.ndla.audioapi.service.search.SearchService
import no.ndla.audioapi.AudioApiProperties.{MaxAudioFileSizeBytes, RoleWithWriteAccess}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization.read
import org.scalatra.swagger._
import org.scalatra._
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger.DataType.ValueDataType

import scala.util.{Failure, Success, Try}

trait AudioController {
  this: AudioRepository with ReadService with WriteService with SearchService with AuthenticationRole with Clock=>
  val audioApiController: AudioController

  class AudioController(implicit val swagger: Swagger) extends NdlaController with FileUploadSupport with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for accessing audio from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()
    registerModel[NewAudioMetaInformation]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val getAudioFiles =
      (apiOperation[SearchResult]("getAudioFiles")
        summary "Show all audio files"
        notes "Shows all the audio files in the ndla.no database. You can search it too."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        queryParam[Option[String]]("query").description("Return only audio with titles or tags matching the specified query."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[String]]("license").description("Return only audio with provided license."),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

    val getByAudioId =
      (apiOperation[AudioMetaInformation]("findByAudioId")
        summary "Show audio info"
        notes "Shows info of the audio with submitted id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("id").description("Audio_id of the audio that needs to be fetched.")
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

    val newAudio =
      (apiOperation[AudioMetaInformation]("newAudio")
        summary "Upload a new audio file with meta data"
        notes "Upload a new audio file with meta data"
        consumes "multipart/form-data"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        formParam[String]("metadata").description("The metadata for the audio file to submit. See NewAudioMetaInformation."),
        Parameter(name = "files", `type` = ValueDataType("file"), description = Some("The image file(s) to upload"), paramType = ParamType.Form)
        )
        authorizations "oauth2"
        responseMessages(response400, response403, response500))

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxAudioFileSizeBytes)))

    get("/", operation(getAudioFiles)) {
      val query = paramOrNone("query")
      val language = paramOrNone("language")
      val license = paramOrNone("license")
      val sort = paramOrNone("sort")
      val pageSize = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)

      query match {
        case Some(q) => searchService.matchingQuery(
          query = q.toLowerCase().split(" ").map(_.trim),
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

    get("/:id", operation(getByAudioId)) {
      val id = long("id")
      readService.withId(id) match {
        case Some(audio) => audio
        case None => NotFound(Error(Error.NOT_FOUND, s"Audio with id $id not found"))
      }
    }

    post("/", operation(newAudio)) {
      authRole.assertHasRole(RoleWithWriteAccess)

      val newAudio = params.get("metadata")
        .map(extract[NewAudioMetaInformation])
        .getOrElse(throw new ValidationException(errors=Seq(ValidationMessage("metadata", "The request must contain audio metadata"))))
      val files = fileMultiParams.getOrElse("files", throw new ValidationException(errors=Seq(ValidationMessage("files", "The request must contain one or more files"))))

      writeService.storeNewAudio(newAudio, files) match {
        case Success(audioMeta) => audioMeta
        case Failure(e) => errorHandler(e)
      }
    }

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

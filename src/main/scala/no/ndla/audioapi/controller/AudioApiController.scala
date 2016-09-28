/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.api.{AudioMetaInformation, Error, SearchResult}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.ReadService
import no.ndla.audioapi.service.search.SearchService
import org.scalatra.swagger.{Swagger, SwaggerSupport}
import org.scalatra._

import scala.util.Try

trait AudioApiController {
  this: AudioRepository with ReadService with SearchService =>
  val audioApiController: AudioApiController

  class AudioApiController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected val applicationDescription = "API for accessing audio from ndla.no."

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
        ))

    val getByAudioId =
      (apiOperation[AudioMetaInformation]("findByAudioId")
        summary "Show audio info"
        notes "Shows info of the audio with submitted id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("audio_id").description("Audio_id of the audio that needs to be fetched."))
        )

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

  }
}

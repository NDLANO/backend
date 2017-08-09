/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.Language
import no.ndla.audioapi.model.api.Title
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{IndexService, SearchIndexService}
import no.ndla.audioapi.service.{ConverterService, ImportService, ReadService}
import org.scalatra.{InternalServerError, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: ImportService with ConverterService with SearchIndexService with AudioRepository with IndexService with ReadService =>
  val internController: InternController

  class InternController extends NdlaController {

    get("/external/:external_id") {
      readService.withExternalId(params("external_id"), paramOrNone("language"))
    }

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) => {
          val result = s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    post("/import/:external_id") {
      for {
        imported <- importService.importAudio(params("external_id"))
        indexed <- searchIndexService.indexDocument(imported)
        audio <- converterService.toApiAudioMetaInformation(indexed, None)
      } yield audio
    }

  }
}

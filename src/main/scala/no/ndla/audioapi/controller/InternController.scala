/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

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
      readService.withExternalId(params("external_id"))
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
      val externalId = params("external_id")
      val importedDocument = for {
        imported <- importService.importAudio(externalId)
        indexed <- searchIndexService.indexDocument(imported)
      } yield indexed

      importedDocument match {
        case Success(audio) => converterService.toApiAudioMetaInformation(audio)
        case Failure(ex) => {
          val errorMessage = s"Import of audio with external_id $externalId failed: ${ex.getMessage}"
          logger.warn(errorMessage, ex)
          InternalServerError(errorMessage)
        }
      }
    }
  }
}

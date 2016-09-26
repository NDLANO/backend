/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.repository.AudioRepositoryComponent
import no.ndla.audioapi.service.search.{ElasticContentIndexComponent, SearchIndexServiceComponent}
import no.ndla.audioapi.service.{ConverterService, ImportServiceComponent}
import org.scalatra.{InternalServerError, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: ImportServiceComponent with ConverterService with SearchIndexServiceComponent with AudioRepositoryComponent with ElasticContentIndexComponent =>
  val internController: InternController

  class InternController extends NdlaController {

    post("/index") {
      Ok(searchIndexService.indexDocuments())
    }

    post("/import/:external_id") {
      val externalId = params("external_id")
      importService.importAudio(externalId) match {
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

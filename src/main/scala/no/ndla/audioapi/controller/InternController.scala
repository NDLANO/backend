/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.auth.User
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{IndexService, SearchIndexService}
import no.ndla.audioapi.service.{ConverterService, ImportService, ReadService}
import org.scalatra.{InternalServerError, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: ImportService
    with ConverterService
    with SearchIndexService
    with AudioRepository
    with IndexService
    with ReadService
    with User =>
  val internController: InternController

  class InternController extends NdlaController {

    get("/external/:external_id") {
      readService.withExternalId(params("external_id"), paramOrNone("language"))
    }

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) => {
          val result =
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    delete("/index") {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      val deleteResults = indexService.findAllIndexes(AudioApiProperties.SearchIndex) match {
        case Failure(f) => halt(status = 500, body = f.getMessage)
        case Success(indexes) =>
          indexes.map(index => {
            logger.info(s"Deleting index $index")
            indexService.deleteIndexWithName(Option(index))
          })
      }
      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
      }
    }

    post("/import/:external_id") {
      authUser.assertHasId()
      for {
        imported <- importService.importAudio(params("external_id"))
        indexed <- searchIndexService.indexDocument(imported)
        audio <- converterService.toApiAudioMetaInformation(indexed, None)
      } yield audio
    }

  }
}

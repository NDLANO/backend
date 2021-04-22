/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.ComponentRegistry.{audioIndexService, seriesController}
import no.ndla.audioapi.auth.User
import no.ndla.audioapi.model.api.NotFoundException
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{AudioIndexService, SeriesIndexService, TagIndexService}
import no.ndla.audioapi.service.{ConverterService, ImportService, ReadService}
import org.scalatra.{InternalServerError, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: ImportService
    with AudioIndexService
    with ConverterService
    with AudioRepository
    with AudioIndexService
    with SeriesIndexService
    with TagIndexService
    with ReadService
    with User =>
  val internController: InternController

  class InternController extends NdlaController {

    get("/external/:external_id") {
      readService.withExternalId(params("external_id"), paramOrNone("language"))
    }

    post("/index") {
      (audioIndexService.indexDocuments, tagIndexService.indexDocuments, seriesIndexService.indexDocuments) match {
        case (Success(audioReindexResult), Success(tagReindexResult), Success(seriesReIndexResult)) => {
          val result =
            s"""Completed indexing of ${audioReindexResult.totalIndexed} documents in ${audioReindexResult.millisUsed} (audios) ms.
               |Completed indexing of ${tagReindexResult.totalIndexed} documents in ${tagReindexResult.millisUsed} (tags) ms.
               |Completed indexing of ${seriesReIndexResult.totalIndexed} documents in ${seriesReIndexResult.millisUsed} (series) ms.""".stripMargin
          logger.info(result)
          Ok(result)
        }
        case (Failure(f), _, _) =>
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        case (_, Failure(f), _) =>
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        case (_, _, Failure(f)) =>
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
      }
    }

    delete("/index") {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      val deleteResults = audioIndexService.findAllIndexes(AudioApiProperties.SearchIndex) match {
        case Failure(f) => halt(status = 500, body = f.getMessage)
        case Success(indexes) =>
          indexes.map(index => {
            logger.info(s"Deleting index $index")
            audioIndexService.deleteIndexWithName(Option(index))
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
        indexed <- audioIndexService.indexDocument(imported)
        _ <- tagIndexService.indexDocument(imported)
        audio <- converterService.toApiAudioMetaInformation(indexed, None)
      } yield audio
    }

    get("/dump/audio/") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)
      readService.getMetaAudioDomainDump(pageNo, pageSize)
    }

    get("/dump/audio/:id") {
      val id = long("id")
      audioRepository.withId(id) match {
        case Some(image) => Ok(image)
        case None        => errorHandler(new NotFoundException(s"Could not find audio with id: '$id'"))
      }
    }

    post("/dump/audio/") {
      val domainMeta = extract[AudioMetaInformation](request.body)
      Ok(audioRepository.insert(domainMeta))
    }

  }
}

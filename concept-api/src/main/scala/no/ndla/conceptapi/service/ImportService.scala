/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.auth.User
import no.ndla.conceptapi.integration.{ArticleApiClient, DomainImageMeta, ImageApiClient}
import no.ndla.conceptapi.model.api.{ConceptImportResults, ImportException}
import no.ndla.conceptapi.model.api.listing.Cover
import no.ndla.conceptapi.repository.DraftConceptRepository
import no.ndla.conceptapi.model.domain
import cats._
import cats.data._
import cats.implicits._
import no.ndla.conceptapi.model.domain.Status

import scala.util.{Failure, Success, Try}

trait ImportService {
  this: ConverterService
    with Clock
    with User
    with WriteService
    with DraftConceptRepository
    with ArticleApiClient
    with ImageApiClient =>
  val importService: ImportService

  class ImportService extends LazyLogging {

    def importConcepts(forceUpdate: Boolean): Try[ConceptImportResults] = {
      val start = System.currentTimeMillis()
      val pageStream = articleApiClient.getChunks
      pageStream
        .map(page => {
          page.map(successfulPage => {
            val saved = writeService.saveImportedConcepts(successfulPage, forceUpdate)
            val numSuccessfullySaved = saved.count(_.isSuccess)
            val warnings = saved.collect { case Failure(ex) => ex.getMessage }
            (numSuccessfullySaved, successfulPage.size, warnings)
          })
        })
        .toList
        .sequence
        .map(done => handleFinishedImport(start, done))
    }

    private def handleFinishedImport(startTime: Long, successfulPages: List[(Int, Int, Seq[String])]) = {
      draftConceptRepository.updateIdCounterToHighestId()
      val (totalSaved, totalAttempted, allWarnings) = successfulPages.foldLeft((0, 0, Seq.empty[String])) {
        case ((tmpTotalSaved, tmpTotalAttempted, tmpWarnings), (pageSaved, pageAttempted, pageWarnings)) =>
          (tmpTotalSaved + pageSaved, tmpTotalAttempted + pageAttempted, tmpWarnings ++ pageWarnings)
      }

      val usedTime = System.currentTimeMillis() - startTime
      logger.info(s"Successfully saved $totalSaved out of $totalAttempted attempted imported concepts in $usedTime ms.")
      ConceptImportResults(totalSaved, totalAttempted, allWarnings)
    }

  }

}

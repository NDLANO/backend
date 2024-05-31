/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.conceptapi.integration.ArticleApiClient
import no.ndla.conceptapi.model.api.ConceptImportResults
import no.ndla.conceptapi.repository.DraftConceptRepository
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Try}

trait ImportService {
  this: ConverterService with WriteService with DraftConceptRepository with ArticleApiClient =>
  val importService: ImportService

  class ImportService extends StrictLogging {

    def importConcepts(forceUpdate: Boolean, user: TokenUser): Try[ConceptImportResults] = {
      val start      = System.currentTimeMillis()
      val pageStream = articleApiClient.getChunks(user)
      pageStream
        .map(page => {
          page.map(successfulPage => {
            val saved                = writeService.saveImportedConcepts(successfulPage, forceUpdate, user)
            val numSuccessfullySaved = saved.count(_.isSuccess)
            val warnings             = saved.collect { case Failure(ex) => ex.getMessage }
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

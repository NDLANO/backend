/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.frontpage.PopularArticle
import no.ndla.frontpageapi.repository.SubjectPageRepository
import no.ndla.network.clients.matomo.MatomoApiClient

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

class MatomoService(using matomoApiClient: MatomoApiClient, subjectPageRepository: SubjectPageRepository)
    extends StrictLogging {

  private val contextIdPattern = """#([a-f0-9]+)$""".r

  private def extractContextId(url: String): Option[String] = {
    contextIdPattern.findFirstMatchIn(url).map(_.group(1))
  }

  def updatePopularArticlesForSubject(subjectId: Long, subjectSlug: String, limit: Int = 20): Try[Int] = {
    val today     = LocalDate.now()
    val weekAgo   = today.minusDays(7)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dateRange = s"${weekAgo.format(formatter)},${today.format(formatter)}"

    for {
      matomoResults  <- matomoApiClient.getTopPageUrlsForSubject(subjectSlug, "range", dateRange, limit)
      popularArticles = matomoResults.flatMap { result =>
        result
          .url
          .map { url =>
            PopularArticle(pageUrl = url, contextId = extractContextId(url), nbHits = result.nb_hits)
          }
      }
      maybeSubject <- subjectPageRepository.withId(subjectId)
      result       <- maybeSubject match {
        case Some(subject) =>
          val updated = subject.copy(popularArticles = popularArticles)
          subjectPageRepository.updateSubjectPage(updated).map(_ => popularArticles.size)
        case None => Failure(new RuntimeException(s"Subject page with id $subjectId not found"))
      }
    } yield result
  }

  def updatePopularArticlesForAllSubjects(subjectMapping: Map[Long, String], limit: Int = 20): Try[Map[Long, Int]] = {
    val results = subjectMapping.map { case (subjectId, subjectSlug) =>
      subjectId -> updatePopularArticlesForSubject(subjectId, subjectSlug, limit)
    }

    val failures = results.collect { case (id, Failure(ex)) =>
      (id, ex)
    }
    failures.foreach { case (id, ex) =>
      logger.error(s"Failed to update popular articles for subject $id", ex)
    }

    val successes = results.collect { case (id, Success(count)) =>
      (id, count)
    }
    Success(successes.toMap)
  }
}

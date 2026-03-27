/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.MissingIdException
import no.ndla.common.implicits.*
import no.ndla.common.model.domain.frontpage.{PopularArticle, SubjectPage}
import no.ndla.common.model.taxonomy.Node
import no.ndla.database.{DBUtility, WriteableDbSession}
import no.ndla.frontpageapi.model.api.PopularArticlesResultDTO
import no.ndla.frontpageapi.repository.SubjectPageRepository
import no.ndla.network.clients.TaxonomyApiClient
import no.ndla.network.clients.matomo.MatomoApiClient

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

class MatomoService(using
    matomoApiClient: MatomoApiClient,
    subjectPageRepository: SubjectPageRepository,
    dbUtility: DBUtility,
    taxonomyApiClient: TaxonomyApiClient,
) extends StrictLogging {

  private val contextIdPattern = """#([a-f0-9]+)$""".r

  private def extractContextId(url: String): Option[String] = {
    // TODO: Parse as url and check that last part is in contextId format
    contextIdPattern.findFirstMatchIn(url).map(_.group(1))
  }

  private def updatePopularArticlesForSubject(subjectPage: SubjectPage, taxSubject: Node, articleLimit: Int)(implicit
      session: WriteableDbSession
  ): Try[Int] = {
    val today     = LocalDate.now()
    val weekAgo   = today.minusDays(7)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dateRange = s"${weekAgo.format(formatter)},${today.format(formatter)}"

    // TODO: Get urlName from taxonomy and then use that to lookup the matomo results
    taxSubject.

    for {
      // TODO: Fix so matomo only needs to lookup the subtable id once instead of once per article/subject

      matomoResults  <- matomoApiClient.getTopPageUrlsForSubject(subjectSlug, "range", dateRange, articleLimit)
      popularArticles = matomoResults.map { result =>
        val pageUrl = s"https://${result.label}"
        PopularArticle(pageUrl = pageUrl, contextId = extractContextId(pageUrl), numHits = result.nb_hits)
      }
      toUpdate  = subject.copy(popularArticles = popularArticles)
      uopdated <- subjectPageRepository.updateSubjectPage(updated).map(_ => popularArticles.size)
    } yield result
  }

  private val articleLimit                                                       = 20
  def updatePopularArticlesForAllSubjects(): Try[List[PopularArticlesResultDTO]] = {

    dbUtility.writeSession { implicit session =>
      permitTry {
        val taxonomySubjects = taxonomyApiClient.getSubjects(true)
        val iterator         = subjectPageRepository.subjectPageIterator.?
        val x                = iterator.map { subjectPagesResult =>
          val subjectPages = subjectPagesResult.?
          subjectPages.map { subjectPage =>
            val updated = updatePopularArticlesForSubject(subjectPage, articleLimit)
            updated
          }
        }
        x
      }
    }
  }
}

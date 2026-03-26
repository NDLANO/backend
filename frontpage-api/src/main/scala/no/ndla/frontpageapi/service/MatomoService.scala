/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.Uri
import no.ndla.common.errors.MissingIdException
import no.ndla.common.implicits.*
import no.ndla.common.model.domain.frontpage.{PopularArticle, SubjectPage}
import no.ndla.common.model.taxonomy.Node
import no.ndla.database.{DBUtility, WriteableDbSession}
import no.ndla.frontpageapi.model.api.PopularArticlesResultDTO
import no.ndla.frontpageapi.repository.SubjectPageRepository
import no.ndla.frontpageapi.service.MatomoService.*
import no.ndla.network.clients.TaxonomyApiClient
import no.ndla.network.clients.matomo.model.MatomoPageUrlResult
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

  def extractContextId(url: String): Option[String] = for {
    uri      <- Uri.parseTry(url).toOption
    parts     = uri.path.parts.filter(_.nonEmpty).toList
    typeIndex = parts.indexWhere(p => p == "e" || p == "r")
    if typeIndex >= 0
    contextId <- contextIdFromPath(parts.drop(typeIndex))
    if ContextIdRegex.matches(contextId)
  } yield contextId

  private def contextIdFromPath(pathFromType: List[String]): Option[String] = pathFromType match {
    case ("e" | "r") :: id :: Nil           => Some(id)
    case "r" :: id :: _ :: Nil              => Some(id)
    case ("e" | "r") :: _ :: _ :: id :: Nil => Some(id)
    case "r" :: _ :: _ :: id :: _ :: Nil    => Some(id)
    case _                                  => None
  }

  private def fetchTopMatomoPagesForSubject(
      subjectId: String,
      dateRange: String,
      subtableIds: Map[String, Long],
      dimensionId: String,
  ): Try[List[MatomoPageUrlResult]] = subtableIds.get(subjectId) match {
    case None             => Failure(MissingMatomoDataEx(s"No Matomo subtable ID found for subject slug '$subjectId'"))
    case Some(subtableId) =>
      matomoApiClient.getTopPageUrlsForSubject(subjectId, MatomoPeriod, dateRange, PageLimit, subtableId, dimensionId)
  }

  private def toPopularArticle(matomoResult: MatomoPageUrlResult): Option[PopularArticle] = {
    val extractedContextId = extractContextId(matomoResult.label)
    extractedContextId.map(ctxId => PopularArticle(ctxId, matomoResult.nb_hits))
  }

  private def fetchAndStorePopularArticlesForSubject(
      subjectPage: SubjectPage,
      taxNode: Node,
      dateRange: String,
      subtableIds: Map[String, Long],
      dimensionId: String,
  )(implicit session: WriteableDbSession): Try[Int] = for {
    topPages           <- fetchTopMatomoPagesForSubject(taxNode.id, dateRange, subtableIds, dimensionId)
    popularArticles     = topPages.flatMap(toPopularArticle).take(TopArticlesLimit)
    subjectPageToUpdate = subjectPage.copy(popularArticles = popularArticles)
    _                  <- subjectPageRepository.updateSubjectPage(subjectPageToUpdate)
  } yield popularArticles.size

  private def lastWeekDateRange: String = {
    val today   = LocalDate.now()
    val weekAgo = today.minusDays(DaysInDateRange)
    s"${weekAgo.format(DateTimeFormatter.ISO_LOCAL_DATE)},${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
  }

  def updatePopularArticlesForSubjectPage(
      contentUriToNode: Map[String, Node],
      subjectPageResult: Try[SubjectPage],
      dateRange: String,
      subtableIds: Map[String, Long],
      dimensionId: String,
  )(implicit session: WriteableDbSession): Try[Option[PopularArticlesResultDTO]] = permitTry {
    val subjectPage   = subjectPageResult.?
    val subjectPageId = subjectPage.id.toTry(MissingIdException("Subjectpage is missing ID")).?
    val contentUri    = s"urn:frontpage:$subjectPageId"
    contentUriToNode.get(contentUri) match {
      case None =>
        logger.info(s"No taxonomy node found for subjectpage $subjectPageId, skips updating popular articles")
        Success(None)
      case Some(taxNode) =>
        fetchAndStorePopularArticlesForSubject(subjectPage, taxNode, dateRange, subtableIds, dimensionId) match {
          case Success(count) =>
            logger.info(s"Updated popular articles for subjectpage $subjectPageId")
            Success(Some(PopularArticlesResultDTO(subjectPageId, count)))
          case Failure(ex: MissingMatomoDataEx) =>
            logger.info(s"Skipped updating popular articles for subjectpage $subjectPageId: ${ex.getMessage}")
            Success(None)
          case Failure(ex) =>
            logger.error(s"Error updating popular articles for subjectpage $subjectPageId", ex)
            Failure(ex)
        }
    }
  }

  private def updatePopularArticlesForEachSubjectPage(
      contentUriToNode: Map[String, Node],
      dateRange: String,
      subtableIds: Map[String, Long],
      dimensionId: String,
  )(implicit session: WriteableDbSession) = subjectPageRepository
    .subjectPageIterator
    .to(LazyList)
    .traverse(page => updatePopularArticlesForSubjectPage(contentUriToNode, page, dateRange, subtableIds, dimensionId))
    .map(_.flatten.toList)

  def updatePopularArticlesForAllSubjects(): Try[List[PopularArticlesResultDTO]] = dbUtility.writeSession {
    implicit session =>
      for {
        taxonomySubjects <- taxonomyApiClient.getSubjects(true)
        contentUriToNode  = taxonomySubjects.flatMap(node => node.contentUri.map(_ -> node)).toMap
        dateRange         = lastWeekDateRange
        dimensionId      <- matomoApiClient.getDimensionIdForSubjectId
        subtableIds      <- matomoApiClient.getSubtableIds(MatomoPeriod, dateRange, dimensionId)
        result           <- updatePopularArticlesForEachSubjectPage(contentUriToNode, dateRange, subtableIds, dimensionId)
      } yield result
  }
}

object MatomoService {
  case class MissingMatomoDataEx(message: String) extends RuntimeException(message)

  private val MatomoPeriod     = "range"
  private val PageLimit        = 100
  private val TopArticlesLimit = 20
  private val DaysInDateRange  = 7
  private val ContextIdRegex   = "[A-Fa-f0-9]{10}([A-Fa-f0-9]{2})?".r
}

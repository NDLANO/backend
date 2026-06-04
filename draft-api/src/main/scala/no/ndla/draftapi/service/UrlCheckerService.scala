/*
 * Part of NDLA draft-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.model.domain.{EditorNote, Responsible, RevisionMeta, RevisionStatus}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.domain.Status
import no.ndla.network.NdlaClient
import org.jsoup.Jsoup
import sttp.client4.quick.*

import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

sealed trait UrlCheckResult
case object UrlOk                               extends UrlCheckResult
case class UrlRedirected(newUrl: String)        extends UrlCheckResult
case class UrlBroken(statusCode: Int)           extends UrlCheckResult
case class UrlCheckFailed(exception: Throwable) extends UrlCheckResult

class UrlCheckerService(using ndlaClient: NdlaClient, clock: Clock) extends StrictLogging {

  private[service] def extractUrls(html: String): List[String] = {
    val doc        = Jsoup.parseBodyFragment(html)
    val anchorUrls = doc.select("a[href]").asScala.toList.map(_.attr("href"))
    val embedUrls  = doc
      .select("ndlaembed[data-resource=related-content][data-url]")
      .asScala
      .toList
      .map(_.attr("data-url"))
    (
      anchorUrls ++ embedUrls
    ).filter(url => url.startsWith("http://") || url.startsWith("https://"))
  }

  private[service] def checkUrl(url: String): UrlCheckResult = {
    Try(ndlaClient.client.send(quickRequest.head(uri"$url").followRedirects(false))) match {
      case Failure(ex) =>
        logger.warn(s"Failed to check URL '$url': ${ex.getMessage}")
        UrlCheckFailed(ex)
      case Success(response) =>
        val code = response.code.code
        if (code == 200) {
          UrlOk
        } else if (Seq(301, 307, 308).contains(code)) {
          response.header("Location") match {
            case Some(location) => UrlRedirected(location)
            case None           =>
              logger.warn(s"Redirect response $code for URL '$url' had no Location header")
              UrlOk
          }
        } else if (code >= 400 && code < 600) {
          UrlBroken(code)
        } else {
          UrlOk
        }
    }
  }

  private[service] def updateContentUrls(content: String, urlUpdates: Map[String, String]): String = {
    if (urlUpdates.isEmpty) content
    else {
      val doc = Jsoup.parseBodyFragment(content)
      doc
        .select("a[href]")
        .asScala
        .foreach { element =>
          val href = element.attr("href")
          urlUpdates.get(href).foreach(newUrl => element.attr("href", newUrl))
        }
      doc
        .select("ndlaembed[data-resource=related-content][data-url]")
        .asScala
        .foreach { element =>
          val dataUrl = element.attr("data-url")
          urlUpdates.get(dataUrl).foreach(newUrl => element.attr("data-url", newUrl))
        }
      doc.body().html()
    }
  }

  def checkAndUpdateUrls(draft: Draft, user: String): Draft = {
    val allUrls = draft.content.flatMap(ac => extractUrls(ac.content)).distinct.toList
    if (allUrls.isEmpty) return draft

    logger.info(s"Checking ${allUrls.size} URL(s) in draft ${draft.id.getOrElse("unknown")}")

    val results: Map[String, UrlCheckResult] = allUrls.map(url => url -> checkUrl(url)).toMap

    val urlRedirects: Map[String, String] = results.collect { case (url, UrlRedirected(newUrl)) =>
      url -> newUrl
    }

    val brokenUrls: List[(String, String)] = results
      .collect {
        case (url, UrlBroken(code))           => url -> s"URL '$url' returnerte HTTP $code og må oppdateres."
        case (url, UrlCheckFailed(exception)) =>
          url -> s"URL '$url' kunne ikke nås (${exception.getMessage}) og må oppdateres."
      }
      .toList

    // Update content with new URLs for redirects
    val updatedContent =
      if (urlRedirects.nonEmpty) draft.content.map(ac => ac.copy(content = updateContentUrls(ac.content, urlRedirects)))
      else draft.content

    // Add EditorNotes for redirected URLs
    val redirectNotes: Seq[EditorNote] = urlRedirects
      .map { case (oldUrl, newUrl) =>
        EditorNote(s"URL '$oldUrl' ble automatisk oppdatert til '$newUrl'.", user, draft.status, clock.now())
      }
      .toSeq

    // Add RevisionMeta for broken/unreachable URLs – only if a note for that URL does not already exist
    val existingRevisionNotes              = draft.revisionMeta.map(_.note).toSet
    val newRevisionMeta: Seq[RevisionMeta] = brokenUrls.flatMap { case (_, note) =>
      if (existingRevisionNotes.contains(note)) None
      else Some(
        RevisionMeta(
          id = UUID.randomUUID(),
          revisionDate = clock.now().plusMonths(1).withNano(0),
          note = note,
          status = RevisionStatus.NeedsRevision,
        )
      )
    }

    val updated = draft.copy(
      content = updatedContent,
      notes = draft.notes ++ redirectNotes,
      revisionMeta = draft.revisionMeta ++ newRevisionMeta,
    )
    // Adding revisionMeta alone (broken/unreachable links) does not constitute a meaningful
    // change that requires republishing — only content/notes updates (redirects) do.
    val draftContentUpdated = updated.copy(revisionMeta = draft.revisionMeta) != draft
    changeStatusIfNeeded(updated, draftContentUpdated)
  }

  private def changeStatusIfNeeded(draft: Draft, changed: Boolean): Draft = {
    if (changed && draft.status.current == DraftStatus.PUBLISHED) {
      val inProgressStatus = Status(DraftStatus.IN_PROGRESS, draft.status.other)
      val newResponsible   = Responsible(responsibleId = draft.updatedBy, lastUpdated = clock.now())
      logger.info(
        s"Draft ${draft.id.getOrElse("unknown")} has been modified by URL check — changing status and setting responsible to '${draft.updatedBy}'"
      )
      draft.copy(status = inProgressStatus, responsible = Some(newResponsible))
    } else draft
  }
}

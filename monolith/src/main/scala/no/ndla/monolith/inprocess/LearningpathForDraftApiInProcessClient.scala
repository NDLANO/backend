/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.common.model.domain.Title
import no.ndla.draftapi.integration.{LearningPath, LearningpathApiClient}
import no.ndla.network.tapir.auth.TokenUser

import scala.util.Try

/** In-process implementation of draft-api's [[LearningpathApiClient]] trait that delegates to learningpath-api's
  * [[no.ndla.learningpathapi.service.search.SearchService]] directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.learningpathapi.ComponentRegistry]] and [[no.ndla.draftapi.ComponentRegistry]] in the monolith.
  */
class LearningpathForDraftApiInProcessClient(learningpathApiCr: => no.ndla.learningpathapi.ComponentRegistry)
    extends LearningpathApiClient {

  override def getLearningpathsWithId(articleId: Long, user: TokenUser): Try[Seq[LearningPath]] = learningpathApiCr
    .searchService
    .containsArticle(articleId)
    .map { summaries =>
      summaries.map { s =>
        // The HTTP path returns a Seq[LearningPathSummaryV2DTO] which the draft-api client decodes into its narrower
        // LearningPath(id, title) shape via Circe's tolerant Decoder. Mirror that projection explicitly here.
        LearningPath(s.id, Title(s.title.title, s.title.language))
      }
    }
}

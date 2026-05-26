/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import com.typesafe.scalalogging.StrictLogging
import no.ndla.myndlaapi.integration.SearchApiClient

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** In-process implementation of myndla-api's [[SearchApiClient]] trait that delegates to search-api's per-type index
  * services directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.searchapi.ComponentRegistry]] and [[no.ndla.myndlaapi.ComponentRegistry]] in the monolith.
  *
  * Mirrors the async fire-and-forget semantics the HTTP impl exposes — each reindex call returns immediately and logs
  * success/failure on a dedicated single-thread executor.
  */
class SearchForMyndlaApiInProcessClient(producerCr: => no.ndla.searchapi.ComponentRegistry)
    extends SearchApiClient
    with StrictLogging {

  private def reindexAsync(id: String, documentType: String, run: Long => Try[?]): Unit = {
    val ec     = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    val future = Future {
      Try(id.toLong).flatMap(run)
    }(using ec)

    future.onComplete { result =>
      result.flatten match {
        case Success(_) => logger.info(s"Successfully reindexed '$documentType' with id '$id'")
        case Failure(e) => logger.error(s"Failed to reindex '$documentType' with id '$id'", e)
      }
    }(using ec)
  }

  override def reindexDraft(id: String): Unit =
    reindexAsync(id, "draft", producerCr.draftIndexService.reindexDocument(_))

  override def reindexLearningpath(id: String): Unit =
    reindexAsync(id, "learningpath", producerCr.learningPathIndexService.reindexDocument(_))

  override def reindexConcept(id: String): Unit =
    reindexAsync(id, "concept", producerCr.draftConceptIndexService.reindexDocument(_))
}

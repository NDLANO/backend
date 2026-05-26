/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.learningpathapi.integration.SearchApiClient
import no.ndla.network.tapir.auth.TokenUser

import java.util.concurrent.Executors
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

/** In-process implementation of learningpath-api's [[SearchApiClient]] trait that delegates to search-api's
  * [[no.ndla.searchapi.service.search.LearningPathIndexService]] directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.searchapi.ComponentRegistry]] and [[no.ndla.learningpathapi.ComponentRegistry]] in the monolith.
  *
  * Mirrors the async fire-and-forget logging the HTTP impl performs in `indexLearningPathDocument`, so callers see the
  * same observability characteristics they get over the network.
  */
class SearchForLearningpathApiInProcessClient(producerCr: => no.ndla.searchapi.ComponentRegistry)
    extends SearchApiClient
    with StrictLogging {

  override def deleteLearningPathDocument(
      id: Long,
      @unused
      user: Option[TokenUser],
  ): Try[?] = {
    producerCr.learningPathIndexService.deleteDocument(id)
  }

  override def indexLearningPathDocument(
      document: LearningPath,
      @unused
      user: Option[TokenUser],
  ): Future[Try[?]] = {
    val idString                              = document.id.map(_.toString).getOrElse("<missing id>")
    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    val future: Future[Try[?]]                = Future {
      producerCr.learningPathIndexService.indexDocument(document)
    }

    future.onComplete {
      case Success(result) => result match {
          case Failure(ex) =>
            logger.error(s"Failed when calling search-api for indexing '$idString': '${ex.getMessage}'", ex)
          case Success(_) => logger.info(s"Successfully called search-api for indexing '$idString'")
        }
      case Failure(ex) =>
        logger.error(s"Future failed when calling search-api for indexing '$idString': '${ex.getMessage}'", ex)
    }

    future
  }
}

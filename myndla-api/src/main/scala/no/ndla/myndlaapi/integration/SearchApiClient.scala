/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.myndlaapi.Props
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient & Props =>

  val searchApiClient: SearchApiClient

  class SearchApiClient extends StrictLogging {
    private val internEndpoint = s"${props.SearchApiUrl}/intern"

    private def reindex(id: String, documentType: String): Try[Unit] = {
      val req =
        quickRequest
          .post(uri"$internEndpoint/reindex/$documentType/$id")
          .readTimeout(60.seconds)
      ndlaClient.fetchRaw(req).map(_ => ())
    }

    private def reindexAsync(id: String, documentType: String): Unit = {
      val ec     = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val future = Future { reindex(id, documentType) }(ec)

      future.onComplete { result =>
        result.flatten match {
          case Success(_) => logger.info(s"Successfully reindexed '$documentType' with id '$id'")
          case Failure(e) => logger.error(s"Failed to reindex '$documentType' with id '$id'", e)
        }
      }(ec)
    }

    def reindexDraft(id: String): Unit        = reindexAsync(id, "draft")
    def reindexLearningpath(id: String): Unit = reindexAsync(id, "learningpath")
  }
}

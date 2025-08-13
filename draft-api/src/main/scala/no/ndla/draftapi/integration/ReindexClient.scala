/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.Props

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sttp.client3.quick._

trait ReindexClient {
  this: Props =>
  lazy val reindexClient: ReindexClient

  class ReindexClient extends StrictLogging {
    private def reindexArticles() = {
      val req = quickRequest.post(uri"${props.internalApiUrls("article-api")}/index")
      simpleHttpClient.send(req)
    }

    private def reindexAudios() = {
      val req = quickRequest.post(uri"${props.internalApiUrls("audio-api")}/index")
      simpleHttpClient.send(req)
    }

    private def reindexDrafts() = {
      val req = quickRequest.post(uri"${props.internalApiUrls("draft-api")}/index")
      simpleHttpClient.send(req)
    }

    private def reindexImages() = {
      val req = quickRequest.post(uri"${props.internalApiUrls("image-api")}/index")
      simpleHttpClient.send(req)
    }

    def reindexAll(): Future[Unit] = {
      logger.info("Calling for API's to reindex")
      Future {
        reindexArticles(): Unit
        reindexAudios(): Unit
        reindexDrafts(): Unit
        reindexImages(): Unit
      }
    }
  }

}

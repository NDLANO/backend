/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.network.NdlaClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http.Http

trait ReindexClient {
  val reindexClient: ReindexClient

  class ReindexClient extends LazyLogging {

    private def reindexArticles() =
      Http(s"${DraftApiProperties.internalApiUrls("article-api")}/index").postForm.execute()

    private def reindexAudios() = Http(s"${DraftApiProperties.internalApiUrls("audio-api")}/index").postForm.execute()

    private def reindexDrafts() = Http(s"${DraftApiProperties.internalApiUrls("draft-api")}/index").postForm.execute()

    private def reindexImages() = Http(s"${DraftApiProperties.internalApiUrls("image-api")}/index").postForm.execute()

    def reindexAll() = {
      logger.info("Calling for API's to reindex")
      Future {
        reindexArticles()
        reindexAudios()
        reindexDrafts()
        reindexImages()
      }
    }
  }

}

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.Props
import scalaj.http.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ReindexClient {
  this: Props =>
  val reindexClient: ReindexClient

  class ReindexClient extends StrictLogging {
    import props.internalApiUrls

    private def reindexArticles() =
      Http(s"${internalApiUrls("article-api")}/index").postForm.execute()

    private def reindexAudios() = Http(s"${internalApiUrls("audio-api")}/index").postForm.execute()

    private def reindexDrafts() = Http(s"${internalApiUrls("draft-api")}/index").postForm.execute()

    private def reindexImages() = Http(s"${internalApiUrls("image-api")}/index").postForm.execute()

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

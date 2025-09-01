/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class ReindexClient(using props: DraftApiProperties, ndlaClient: NdlaClient) extends StrictLogging {
  private val reindexTimeout = 40.minutes

  def reindex(indexName: String): Try[String] = {
    val body = s"""{"index_name": "$indexName"}"""

    val req = quickRequest
      .post(uri"${props.SearchServer}/api/search/reindex")
      .readTimeout(reindexTimeout)
      .body(body)
      .header("content-type", "application/json")

    ndlaClient.fetchRaw(req) match {
      case Success(response) =>
        val message = s"Reindex called for '$indexName', got '${response.code}'"
        logger.info(message)
        Success(message)

      case Failure(ex) =>
        val message = s"Failed to call reindex for '$indexName'"
        logger.error(message, ex)
        Failure(ex)
    }
  }
}

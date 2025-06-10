/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import no.ndla.common.model.api.learningpath.LearningPathStatsDTO
import no.ndla.myndlaapi.Props
import no.ndla.network.NdlaClient
import sttp.client3.{UriContext, quickRequest}

import scala.concurrent.duration.DurationInt
import scala.util.Try

trait LearningPathApiClient {
  this: NdlaClient & Props =>

  val learningPathApiClient: LearningPathApiClient
  class LearningPathApiClient extends StrictLogging {
    import props.LearningpathApiUrl
    private val learningPathTimeout = 20.seconds

    def getStats: Try[LearningPathStatsDTO] = get[LearningPathStatsDTO](s"$LearningpathApiUrl/intern/stats")

    private def get[A: Decoder](url: String, params: (String, String)*): Try[A] = {
      val request = quickRequest.get(uri"$url".withParams(params*)).readTimeout(learningPathTimeout)
      ndlaClient.fetchWithForwardedAuth[A](request, None)
    }
  }
}

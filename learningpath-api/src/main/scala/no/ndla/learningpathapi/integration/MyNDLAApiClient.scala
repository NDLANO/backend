/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.Props
import no.ndla.myndla.model.api.config.ConfigMetaRestricted
import no.ndla.myndla.model.domain.config.ConfigKey
import no.ndla.network.NdlaClient
import no.ndla.network.model.NdlaRequest
import sttp.client3.quick._

import scala.util.{Failure, Success, Try}

trait MyNDLAApiClient {
  this: Props with NdlaClient =>

  val myndlaApiClient: MyNDLAApiClient

  class MyNDLAApiClient {
    def isWriteRestricted: Try[Boolean] = {
      doRequest(
        quickRequest.get(
          uri"http://${props.MyNDLAApiHost}/myndla-api/v1/config/${ConfigKey.LearningpathWriteRestricted.entryName}"
        )
      ).flatMap(_.value match {
        case Left(value) => Success(value)
        case _           => Failure(new RuntimeException("Expected boolean, got list"))
      })
    }
    private def doRequest(httpRequest: NdlaRequest): Try[ConfigMetaRestricted] = {
      ndlaClient.fetchWithForwardedAuth[ConfigMetaRestricted](httpRequest, None)
    }

  }
}

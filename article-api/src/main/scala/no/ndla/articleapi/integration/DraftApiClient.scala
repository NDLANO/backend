/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.model.api
import no.ndla.common.model.NDLADate
import no.ndla.network.NdlaClient
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import sttp.client3.quick._

import scala.util.{Failure, Success}

trait DraftApiClient {
  this: NdlaClient with Props =>
  val draftApiClient: DraftApiClient

  class DraftApiClient(DraftBaseUrl: String = props.DraftApiUrl) extends StrictLogging {
    def agreementExists(agreementId: Long): IO[Boolean] = getAgreementCopyright(agreementId).map(_.nonEmpty)

    def getAgreementCopyright(agreementId: Long): IO[Option[api.Copyright]] = {
      implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer

      val request = quickRequest.get(uri"$DraftBaseUrl/draft-api/v1/agreements/$agreementId")
      ndlaClient.fetchWithForwardedAuth[Agreement](request).attempt.map {
        case Right(a) =>
          Some(a.copyright)
        case Left(ex) =>
          logger.error("", ex)
          None
      }
    }
  }
}

case class Agreement(id: Long, copyright: api.Copyright)

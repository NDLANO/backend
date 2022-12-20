/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.model.api
import no.ndla.network.NdlaClient
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import scalaj.http.{Http, HttpRequest}

import scala.util.{Failure, Success}

trait DraftApiClient {
  this: NdlaClient with Props =>
  val draftApiClient: DraftApiClient

  class DraftApiClient(DraftBaseUrl: String = props.DraftApiUrl) extends StrictLogging {
    private val draftApiGetAgreementEndpoint = s"$DraftBaseUrl/draft-api/v1/agreements/:agreement_id"

    def agreementExists(agreementId: Long): Boolean = getAgreementCopyright(agreementId).nonEmpty

    def getAgreementCopyright(agreementId: Long): Option[api.Copyright] = {
      implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all
      val request: HttpRequest = Http(s"$draftApiGetAgreementEndpoint".replace(":agreement_id", agreementId.toString))
      ndlaClient.fetchWithForwardedAuth[Agreement](request) match {
        case Success(a) =>
          Some(a.copyright)
        case Failure(ex) =>
          logger.error("", ex)
          None
      }
    }
  }
}

case class Agreement(id: Long, copyright: api.Copyright)

/*
 * Part of NDLA audio-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.integration

import java.util.Date

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.api
import no.ndla.network.NdlaClient

import scala.util.{Success, Try}
import scalaj.http.{Http, HttpRequest}

trait DraftApiClient {
  this: NdlaClient =>
  val draftApiClient: DraftApiClient

  class DraftApiClient {
    private val draftApiGetAgreementEndpoint =
      s"http://${AudioApiProperties.DraftApiHost}/draft-api/v1/agreements/:agreement_id"

    def getAgreementCopyright(agreementId: Long): Option[api.Copyright] = {
      implicit val formats = org.json4s.DefaultFormats
      val request: HttpRequest = Http(s"$draftApiGetAgreementEndpoint".replace(":agreement_id", agreementId.toString))
      ndlaClient.fetchWithForwardedAuth[Agreement](request).toOption match {
        case Some(a) => Some(a.copyright)
        case _       => None
      }
    }

    def agreementExists(agreementId: Long): Boolean = getAgreementCopyright(agreementId).nonEmpty
  }
}

case class Agreement(id: Long,
                     title: String,
                     content: String,
                     copyright: api.Copyright,
                     created: Date,
                     updated: Date,
                     updatedBy: String)

/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.api
import no.ndla.network.NdlaClient
import scalaj.http.{Http, HttpRequest}

trait DraftApiClient {
  this: NdlaClient =>
  val draftApiClient: DraftApiClient

  class DraftApiClient(DraftBaseUrl: String = s"http://${ArticleApiProperties.DraftHost}") {
    private val draftApiGetAgreementEndpoint = s"$DraftBaseUrl/draft-api/v1/agreements/:agreement_id"

    def agreementExists(agreementId: Long): Boolean = getAgreementCopyright(agreementId).nonEmpty

    def getAgreementCopyright(agreementId: Long): Option[api.Copyright] = {
      implicit val formats = org.json4s.DefaultFormats
      val request: HttpRequest = Http(s"$draftApiGetAgreementEndpoint".replace(":agreement_id", agreementId.toString))
      ndlaClient.fetchWithForwardedAuth[Agreement](request).toOption match {
        case Some(a) => Some(a.copyright)
        case _       => None
      }
    }
  }
}

case class Agreement(id: Long, copyright: api.Copyright)

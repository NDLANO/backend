/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import java.util.Date

import no.ndla.network.NdlaClient
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.model.domain.{ArticleApiSearchResults, Author, SearchParams, article}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scalaj.http.{Http, HttpRequest}

trait DraftApiClient {
  this: NdlaClient with SearchApiClient =>
  val draftApiClient: DraftApiClient

  class DraftApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath = "draft-api/v1/drafts"
    override val name = "articles"
    override val dumpDomainPath = "intern/dump/article"

    def search(searchParams: SearchParams)(
        implicit executionContext: ExecutionContext): Future[Try[ArticleApiSearchResults]] =
      search[ArticleApiSearchResults](searchParams)

    private val draftApiGetAgreementEndpoint =
      s"http://${SearchApiProperties.DraftApiUrl}/draft-api/v1/agreements/:agreement_id"

    def agreementExists(agreementId: Long): Boolean =
      getAgreementCopyright(agreementId).nonEmpty

    def getAgreementCopyright(agreementId: Long): Option[article.Copyright] = {
      implicit val formats = org.json4s.DefaultFormats
      val request: HttpRequest = Http(s"$draftApiGetAgreementEndpoint".replace(":agreement_id", agreementId.toString))
      ndlaClient.fetchWithForwardedAuth[Agreement](request).toOption match {
        case Some(a) => Some(a.copyright.toDomainCopyright)
        case _       => None
      }
    }

    case class ApiCopyright(license: License,
                            origin: String,
                            creators: Seq[Author],
                            processors: Seq[Author],
                            rightsholders: Seq[Author],
                            agreementId: Option[Long],
                            validFrom: Option[Date],
                            validTo: Option[Date]) {

      def toDomainCopyright: article.Copyright = {
        article.Copyright(license.license, origin, creators, processors, rightsholders, agreementId, validFrom, validTo)
      }
    }

    case class License(license: String, description: Option[String], url: Option[String])

    case class Agreement(id: Long,
                         title: String,
                         content: String,
                         copyright: ApiCopyright,
                         created: Date,
                         updated: Date,
                         updatedBy: String)
  }

}

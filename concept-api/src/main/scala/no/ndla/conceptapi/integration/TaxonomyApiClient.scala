/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration

import no.ndla.conceptapi.Props
import no.ndla.conceptapi.integration.model.{TaxonomyData, TaxonomySubject}
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt
import scala.util.Try

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient {
    import props.TaxonomyUrl
    implicit val formats: Formats   = SearchableLanguageFormats.JSonFormatsWithMillis
    private val TaxonomyApiEndpoint = s"$TaxonomyUrl/v1"
    private val timeoutSeconds      = 600.seconds

    def getSubjects: Try[TaxonomyData] = {
      get[List[TaxonomySubject]](
        s"$TaxonomyApiEndpoint/nodes/",
        headers = Map(TAXONOMY_VERSION_HEADER -> defaultVersion),
        Seq("nodeType" -> "SUBJECT")
      ).map(TaxonomyData.from)
    }

    private def get[A](url: String, headers: Map[String, String], params: Seq[(String, String)])(implicit
        mf: Manifest[A],
        formats: Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.get(uri"$url?$params").headers(headers).readTimeout(timeoutSeconds),
        None
      )
    }

  }

}

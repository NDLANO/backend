/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration

import io.circe.Decoder
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.integration.model.{TaxonomyData, TaxonomySubject}
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.util.Try

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient {
    import props.TaxonomyUrl
    private val TaxonomyApiEndpoint = s"$TaxonomyUrl/v1"
    private val timeoutSeconds      = 600.seconds

    def getSubjects: Try[TaxonomyData] = {
      get[List[TaxonomySubject]](
        s"$TaxonomyApiEndpoint/nodes/",
        headers = Map(TAXONOMY_VERSION_HEADER -> defaultVersion),
        Seq("nodeType" -> "SUBJECT")
      ).map(TaxonomyData.from)
    }

    private def get[A: Decoder](url: String, headers: Map[String, String], params: Seq[(String, String)]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.get(uri"$url?$params").headers(headers).readTimeout(timeoutSeconds),
        None
      )
    }

  }

}

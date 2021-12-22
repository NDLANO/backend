/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.dsl._
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.network.NdlaClient
import org.json4s.Formats
import scalaj.http.Http

import scala.util.Try

case class ImageAltText(alttext: String, language: String)
case class DomainImageMeta(id: Long, alttexts: Seq[ImageAltText])

trait ImageApiClient {
  this: NdlaClient with LazyLogging =>
  val imageApiClient: ImageApiClient

  class ImageApiClient {
    val baseUrl = s"http://${ConceptApiProperties.ImageApiHost}"

    def getImage(urlToImage: String): Try[DomainImageMeta] = {
      get[DomainImageMeta]("intern/domain_image_from_url/", params = Map("url" -> urlToImage), 5000)
    }

    def get[T](path: String, params: Map[String, Any], timeout: Int)(implicit mf: Manifest[T]): Try[T] = {
      implicit val formats: Formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
      ndlaClient.fetchWithForwardedAuth[T](Http((baseUrl / path).addParams(params.toList)).timeout(timeout, timeout))
    }
  }

}

/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.learningpathapi.Props
import no.ndla.network.NdlaClient
import no.ndla.network.model.{HttpRequestException, NdlaRequest}
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.quick.*

import scala.util.{Failure, Success}

trait ImageApiClientComponent {
  this: NdlaClient with Props =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends StrictLogging {
    def imageMetaWithExternalId(externalId: String, user: Option[TokenUser]): Option[ImageMetaInformation] = {
      doRequest(quickRequest.get(uri"http://${props.ImageApiHost}/intern/extern/$externalId"), user)
    }

    def imageMetaOnUrl(url: String): Option[ImageMetaInformation] =
      doRequest(quickRequest.get(uri"$url"), None)

    private def doRequest(httpRequest: NdlaRequest, user: Option[TokenUser]): Option[ImageMetaInformation] = {
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](httpRequest, user) match {
        case Success(metaInfo) => Some(metaInfo)
        case Failure(hre: HttpRequestException) =>
          if (hre.is404) None else throw hre
        case Failure(ex: Throwable) => throw ex
      }
    }
  }

}
case class ImageMetaInformation(id: String, metaUrl: String, imageUrl: String, size: Int, contentType: String)

object ImageMetaInformation {
  implicit val encoder: Encoder[ImageMetaInformation] = deriveEncoder
  implicit val decoder: Decoder[ImageMetaInformation] = deriveDecoder
}

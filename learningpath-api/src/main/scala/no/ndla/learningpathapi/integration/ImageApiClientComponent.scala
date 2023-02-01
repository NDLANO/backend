/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.learningpathapi.Props
import no.ndla.network.NdlaClient
import no.ndla.network.model.{HttpRequestException, NdlaRequest}
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait ImageApiClientComponent {
  this: NdlaClient with Props =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends StrictLogging {
    private val ImageImportTimeout = 10.seconds

    def imageMetaWithExternalId(externalId: String): Option[ImageMetaInformation] = {
      doRequest(quickRequest.get(uri"http://${props.ImageApiHost}/intern/extern/$externalId"))
    }

    def imageMetaOnUrl(url: String): Option[ImageMetaInformation] =
      doRequest(quickRequest.get(uri"$url"))

    def importImage(externalId: String): Option[ImageMetaInformation] =
      doRequest(
        quickRequest
          .post(uri"http://${props.ImageApiHost}/intern/import/$externalId")
          .readTimeout(ImageImportTimeout)
      )

    private def doRequest(httpRequest: NdlaRequest): Option[ImageMetaInformation] = {
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](httpRequest) match {
        case Success(metaInfo) => Some(metaInfo)
        case Failure(hre: HttpRequestException) =>
          if (hre.is404) None else throw hre
        case Failure(ex: Throwable) => throw ex
      }
    }
  }

}
case class ImageMetaInformation(id: String, metaUrl: String, imageUrl: String, size: Int, contentType: String)

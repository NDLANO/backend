/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import io.circe.Decoder
import no.ndla.draftapi.Props
import no.ndla.network.NdlaClient
import sttp.client4.quick.*

import scala.util.Try

class ImageApiHttpClient(using ndlaClient: NdlaClient, props: Props) extends ImageApiClient {
  private val Endpoint = s"http://${props.ImageApiHost}/image-api/v3/images"

  def getImagesWithIds(ids: Seq[String]): Try[Seq[ImageWithCopyright]] = {
    val idsParam = ids.mkString(",")
    get[Seq[ImageWithCopyright]](s"$Endpoint/ids", "ids" -> idsParam)
  }

  private def get[A: Decoder](endpointUrl: String, params: (String, String)*): Try[A] = {
    val request = quickRequest.get(uri"$endpointUrl".withParams(params*))
    ndlaClient.fetch[A](request)
  }

}

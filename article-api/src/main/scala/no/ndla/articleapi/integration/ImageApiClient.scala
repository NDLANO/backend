/*
 * Part of NDLA article-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.articleapi.Props
import no.ndla.common.model.api.CopyrightDTO
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import scala.util.Try

class ImageApiClient(using ndlaClient: NdlaClient, props: Props) {
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
case class ImageWithCopyright(id: String, copyright: CopyrightDTO)

object ImageWithCopyright {
  implicit val encoder: Encoder[ImageWithCopyright] = deriveEncoder
  implicit val decoder: Decoder[ImageWithCopyright] = deriveDecoder
}

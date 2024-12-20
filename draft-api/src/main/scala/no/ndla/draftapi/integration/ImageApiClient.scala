/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.CopyrightDTO
import no.ndla.draftapi.Props
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import scala.util.Try

trait ImageApiClient {
  this: NdlaClient & ConverterService & Props =>
  val imageApiClient: ImageApiClient

  class ImageApiClient {
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
}
case class ImageWithCopyright(id: String, copyright: CopyrightDTO)

object ImageWithCopyright {
  implicit val encoder: Encoder[ImageWithCopyright] = deriveEncoder
  implicit val decoder: Decoder[ImageWithCopyright] = deriveDecoder
}

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
import no.ndla.common.model.api.CopyrightDTO

import scala.util.Try

trait ImageApiClient {
  def getImagesWithIds(ids: Seq[String]): Try[Seq[ImageWithCopyright]]
}

case class ImageWithCopyright(id: String, copyright: CopyrightDTO)

object ImageWithCopyright {
  implicit val encoder: Encoder[ImageWithCopyright] = deriveEncoder
  implicit val decoder: Decoder[ImageWithCopyright] = deriveDecoder
}

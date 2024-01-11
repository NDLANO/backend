/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class RelatedContentLink(title: String, url: String)

object RelatedContentLink {
  implicit val encoder: Encoder[RelatedContentLink] = deriveEncoder[RelatedContentLink]
  implicit val decoder: Decoder[RelatedContentLink] = deriveDecoder[RelatedContentLink]
}

package object domain {
  type RelatedContent = Either[RelatedContentLink, Long]
}

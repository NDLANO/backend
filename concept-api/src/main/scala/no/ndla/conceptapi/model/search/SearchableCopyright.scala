/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.Author

case class SearchableCopyright(
    origin: Option[String],
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author]
)

object SearchableCopyright {
  implicit val encoder: Encoder[SearchableCopyright] = deriveEncoder
  implicit val decoder: Decoder[SearchableCopyright] = deriveDecoder
}

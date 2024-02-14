/*
 * Part of NDLA common.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Author

case class DraftCopyright(
    license: Option[String],
    origin: Option[String],
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author],
    validFrom: Option[NDLADate],
    validTo: Option[NDLADate],
    processed: Boolean
)

object DraftCopyright {
  implicit val encoder: Encoder[DraftCopyright] = deriveEncoder
  implicit val decoder: Decoder[DraftCopyright] = deriveDecoder
}

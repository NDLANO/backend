/*
 * Part of NDLA common.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Author

case class Copyright(
    license: String,
    origin: Option[String],
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author],
    validFrom: Option[NDLADate],
    validTo: Option[NDLADate],
    processed: Boolean
)

object Copyright {
  implicit def encoder: Encoder[Copyright] = io.circe.generic.semiauto.deriveEncoder[Copyright]
  implicit def decoder: Decoder[Copyright] = io.circe.generic.semiauto.deriveDecoder[Copyright]
}

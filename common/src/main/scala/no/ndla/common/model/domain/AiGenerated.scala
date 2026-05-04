/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

enum AiGenerated {
  case Partial,
    Yes,
    No
}

object AiGenerated {
  implicit val schema: Schema[AiGenerated]   = Schema.derivedEnumeration.defaultStringBased
  implicit val encoder: Encoder[AiGenerated] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[AiGenerated] = Decoder
    .decodeString
    .emap { s =>
      AiGenerated.values.find(_.toString == s).toRight(s"Unknown AiGenerated: $s")
    }
}

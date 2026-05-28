/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Codec
import sttp.tapir.Codec.PlainCodec

enum AiGenerated {
  case Partial,
    Yes,
    No
}

object AiGenerated {
  def withNameOption(name: String): Option[AiGenerated] = values.find(_.toString == name)

  implicit val codec: PlainCodec[AiGenerated] =
    Codec.derivedEnumeration[String, AiGenerated](decode = withNameOption, encode = _.toString)

  implicit val encoder: Encoder[AiGenerated] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[AiGenerated] = Decoder
    .decodeString
    .emap { s =>
      withNameOption(s).toRight(s"Unknown AiGenerated: $s")
    }
}

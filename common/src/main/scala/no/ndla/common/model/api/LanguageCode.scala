/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.{Decoder, Encoder}
import no.ndla.language.Language
import sttp.tapir.{Codec, CodecFormat, Schema}

class LanguageCode(value: String) extends StringBasedValue(value) {
  def code: String = value
}
object LanguageCode {
  def apply(value: String): LanguageCode = parse(value)
  def parse(value: String): LanguageCode = {
    new LanguageCode(Language.languageOrParam(value))
  }

  implicit val schema: Schema[LanguageCode]                              = StringBasedValue.schema[LanguageCode]
  implicit val codec: Codec[String, LanguageCode, CodecFormat.TextPlain] = StringBasedValue.codec(LanguageCode.apply)
  implicit val encoder: Encoder[LanguageCode]                            = StringBasedValue.encoder[LanguageCode]
  implicit val decoder: Decoder[LanguageCode]                            = StringBasedValue.decoder(LanguageCode.apply)
}

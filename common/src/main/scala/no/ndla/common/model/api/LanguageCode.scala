/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import no.ndla.language.Language
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

class LanguageCode private (val code: String) {
  override def toString: String = code
}

object LanguageCode {
  def apply(code: String): LanguageCode = parse(code)

  def parse(value: String): LanguageCode = {
    new LanguageCode(Language.languageOrParam(value))
  }

  def fromOptString(s: Option[String]): Option[LanguageCode] = s.map(f => new LanguageCode(f))
  def fromString(s: String): Option[LanguageCode]            = Option(LanguageCode(s))

  implicit val schema: Schema[LanguageCode]            = Schema.string
  implicit val schemaOpt: Schema[Option[LanguageCode]] = Schema.string.asOption

  implicit val codec: Codec[String, LanguageCode, CodecFormat.TextPlain] = Codec.string.mapDecode { value =>
    DecodeResult.Value(parse(value))
  }(_.code)

  implicit val optCodec: Codec[Option[String], Option[LanguageCode], CodecFormat.TextPlain] = {
    Codec
      .id[Option[String], TextPlain](TextPlain(), Schema.string)
      .mapDecode(x =>
        DecodeResult.Value(
          fromOptString(x)
        )
      )(x => x.map(_.code))
  }

  implicit def circeDecoder: Decoder[LanguageCode] = (c: HCursor) =>
    c.as[String].flatMap { str =>
      fromString(str) match {
        case Some(value) => Right(value)
        case None        => Left(DecodingFailure(DecodingFailure.Reason.CustomReason("Not a string"), c))
      }
    }

  implicit val encoder: Encoder[LanguageCode] = Encoder.instance { lc => lc.toString.asJson }
}

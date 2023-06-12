/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

/** Query parameter wrapper class where the parameter should be decoded as [[Option]] if the string is the empty string
  */
case class MaybeNonEmptyString private (underlying: Option[String])
object MaybeNonEmptyString {
  def fromOptString(s: Option[String]): MaybeNonEmptyString = {
    val opt = s.filter(_.nonEmpty)
    new MaybeNonEmptyString(opt)
  }

  implicit val schema: Schema[MaybeNonEmptyString] = Schema.string
  implicit val queryParamCodec: Codec[List[String], MaybeNonEmptyString, CodecFormat.TextPlain] = {
    Codec
      .id[List[String], TextPlain](TextPlain(), Schema.string)
      .mapDecode(x =>
        DecodeResult.Value(
          fromOptString(x.headOption)
        )
      )(_.underlying.toList)
  }
}

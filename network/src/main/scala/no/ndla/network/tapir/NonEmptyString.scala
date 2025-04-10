/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import io.circe.{Decoder, DecodingFailure, Encoder, FailedCursor, HCursor, Json}
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

/** Class that cannot be constructed with an empty string ("") or a whitespace only string (" "), therefore it means
  * that if you have one of these the underlying string is not empty.
  */
class NonEmptyString private (val underlying: String) {
  override def equals(obj: Any): Boolean = obj match {
    case other: NonEmptyString => other.underlying == underlying
    case other: String         => other == underlying
    case _                     => false
  }

  override def toString: String = underlying
}

object NonEmptyString {
  def apply(underlying: String): Option[NonEmptyString] = fromString(underlying)

  private def validateString(underlying: String): Boolean = underlying.trim.nonEmpty

  def fromOptString(s: Option[String]): Option[NonEmptyString] =
    s.filter(validateString).map(f => new NonEmptyString(f))
  def fromString(s: String): Option[NonEmptyString] =
    Option.when(validateString(s))(new NonEmptyString(s))

  implicit val schema: Schema[NonEmptyString]            = Schema.string
  implicit val schemaOpt: Schema[Option[NonEmptyString]] = Schema.string.asOption
  implicit val queryParamCodec: Codec[List[String], Option[NonEmptyString], CodecFormat.TextPlain] = {
    Codec
      .id[List[String], TextPlain](TextPlain(), Schema.string)
      .mapDecode(x =>
        DecodeResult.Value(
          fromOptString(x.headOption)
        )
      )(x => x.map(_.underlying).toList)
  }

  implicit def circeOptionDecoder: Decoder[Option[NonEmptyString]] = Decoder.withReattempt {
    case c: FailedCursor if !c.incorrectFocus => Right(None)
    case c                                    => c.as[Option[String]].map(maybeStr => fromOptString(maybeStr))
  }

  private[tapir] val parseErrorMessage =
    "Tried to parse an empty string as a `NonEmptyString`. The string needs to have length > 0 (Or maybe you wanted `Option[NonEmptyString]`?)"
  private val decodingFailureReason = DecodingFailure.Reason.CustomReason(parseErrorMessage)

  implicit def circeDecoder: Decoder[NonEmptyString] = (c: HCursor) =>
    c.as[String].flatMap { str =>
      fromString(str) match {
        case Some(value) => Right(value)
        case None        => Left(DecodingFailure(decodingFailureReason, c))
      }
    }

  implicit def circeEncoder: Encoder[NonEmptyString] = (a: NonEmptyString) => Json.fromString(a.underlying)

  /** Helpers that should make working with `Option[NonEmptyString]` a bit easier */
  implicit class NonEmptyStringImplicit(self: Option[NonEmptyString]) {
    def underlying: Option[String]                   = self.map(_.underlying)
    def underlyingOrElse(default: => String): String = self.map(_.underlying).getOrElse(default)
  }
}

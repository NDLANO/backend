/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common

import io.circe.{Decoder, Encoder, HCursor, parser}
import io.circe.syntax.*

import scala.util.Try

object CirceUtil {
  def tryParseAs[T](str: String)(implicit d: Decoder[T]): Try[T] = parser.parse(str).toTry.flatMap(_.as[T].toTry)

  /** This might throw an exception! Use with care, probably only use this in tests */
  def unsafeParseAs[T: Decoder](str: String): T = tryParseAs(str).get

  def toJsonString[T: Encoder](obj: T): String = obj.asJson.noSpaces

  /** Helper to simplify making decoders with default values */
  def getOrDefault[T: Decoder](cur: HCursor, key: String, default: T) = {
    cur.downField(key).as[Option[T]].map(_.getOrElse(default))
  }

}

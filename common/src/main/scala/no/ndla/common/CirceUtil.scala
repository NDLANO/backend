/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common

import io.circe.{Decoder, parser}

import scala.util.Try

object CirceUtil {

  def tryParseAs[T](str: String)(implicit d: Decoder[T]): Try[T] = parser.parse(str).toTry.flatMap(_.as[T].toTry)

  /** This might throw an exception! Use with care, probably only use this in tests */
  def unsafeParseAs[T](str: String)(implicit d: Decoder[T]): T = tryParseAs(str).get

}

/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common

import enumeratum.*
import io.circe.syntax.*
import io.circe.*

import scala.util.{Failure, Try}

object CirceUtil {
  // NOTE: Circe's `DecodingFailure` does not include a stack trace, so we wrap it in our own exception
  //       to make it more like other failures.
  case class CirceFailure(message: String) extends RuntimeException(message)
  object CirceFailure {
    def apply(reason: Throwable): Throwable = new CirceFailure(reason.getMessage).initCause(reason)
  }

  def tryParseAs[T](str: String)(implicit d: Decoder[T]): Try[T] = {
    parser
      .parse(str)
      .toTry
      .flatMap(_.as[T].toTry)
      .recoverWith { ex => Failure(CirceFailure(ex)) }
  }

  /** This might throw an exception! Use with care, probably only use this in tests */
  def unsafeParseAs[T: Decoder](str: String): T = tryParseAs(str).get

  def toJsonString[T: Encoder](obj: T): String = obj.asJson.noSpaces

  /** Helper to simplify making decoders with default values */
  def getOrDefault[T: Decoder](cur: HCursor, key: String, default: T) = {
    cur.downField(key).as[Option[T]].map(_.getOrElse(default))
  }

  private val stringDecoder = implicitly[Decoder[String]]

  /** Trait that does the same as `CirceEnum`, but with slightly better error message */
  trait CirceEnumWithErrors[A <: EnumEntry] extends CirceEnum[A] {
    this: Enum[A] =>
    override implicit val circeDecoder: Decoder[A] = (c: HCursor) =>
      stringDecoder(c).flatMap { s =>
        withNameEither(s).left.map { notFound =>
          val enumName = this.getClass.getSimpleName.stripSuffix("$")
          val enumList = s"[${notFound.enumValues.mkString("'", "','", "'")}]"
          val message  = s"'${notFound.notFoundName}' is not a member of enum '$enumName'. Must be one of $enumList"
          DecodingFailure(message, c.history)
        }
      }
  }

}

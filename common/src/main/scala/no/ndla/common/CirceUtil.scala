/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import enumeratum.*
import io.circe.*
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}

import scala.util.{Failure, Try}

object CirceUtil {
  // NOTE: Circe's `DecodingFailure` does not include a stack trace, so we wrap it in our own exception
  //       to make it more like other failures.
  case class CirceFailure(message: String, jsonString: String) extends RuntimeException(message)
  object CirceFailure {
    def apply(jsonString: String, reason: Throwable): Throwable = {
      val message = s"${reason.getMessage}\n$jsonString"
      new CirceFailure(message, jsonString).initCause(reason)
    }
  }

  def tryParse(str: String): Try[Json] = {
    parser
      .parse(str)
      .toTry
      .recoverWith { ex => Failure(CirceFailure(str, ex)) }
  }

  def tryParseAs[T](str: String)(implicit d: Decoder[T]): Try[T] = tryParse(str).flatMap(_.as[T].toTry)

  /** This might throw an exception! Use with care, probably only use this in tests */
  def unsafeParseAs[T: Decoder](str: String): T = tryParseAs(str).get

  def toJsonString[T: Encoder](obj: T): String = obj.asJson.noSpaces

  /** Helper to simplify making decoders with default values */
  def getOrDefault[T: Decoder](cur: HCursor, key: String, default: T): Either[DecodingFailure, T] = {
    cur.downField(key).as[Option[T]].map(_.getOrElse(default))
  }

  def addTypenameDiscriminator(json: Json, clazz: Class[?]): Json = {
    json.mapObject(_.add("typename", Json.fromString(clazz.getSimpleName)))
  }

  inline def deriveEncoderWithTypename[T](using encoder: Encoder[T]): Encoder[T] =
    Encoder.instance[T] { value =>
      val json = encoder.apply(value)
      addTypenameDiscriminator(json, value.getClass)
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

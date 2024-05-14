/*
 * Part of NDLA backend.common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model

import com.scalatsi.TSType
import com.scalatsi.TypescriptType.TSString
import io.circe.{Decoder, DecodingFailure, Encoder, JsonObject}
import io.circe.syntax.*
import no.ndla.language.model.{LanguageField, WithLanguage}
import sttp.tapir.Schema.SName
import sttp.tapir.{FieldName, Schema, SchemaType}
import sttp.tapir.SchemaType.SProductField

import scala.reflect.runtime.universe.*

case class LanguageType[T, SUBFIELD_NAME <: String](value: T, language: String) extends LanguageField[T] {
  def as[NEW_SFN <: String]: LanguageType[T, NEW_SFN]     = LanguageType(value, language)
  def withValue(value: T): LanguageType[T, SUBFIELD_NAME] = this.copy(value = value)

  // TODO: either remove this, or do something smart?
  //       since `value` is generic this is a bit tricky
  override def isEmpty: Boolean = ???
}

object LanguageType {
  import com.scalatsi.dsl.*

  implicit def tsType[SFN <: String](implicit tt: TypeTag[SFN]): TSType[LanguageType[String, SFN]] = {
    val sfn = tt.tpe match {
      case ConstantType(value) if value.value.isInstanceOf[String] =>
        value.value.asInstanceOf[String]
      case nonConstantType =>
        throw new RuntimeException(
          s"Found non-constant type `$nonConstantType` when decoding `LanguageType`. This is a bug."
        )
    }

    TSType.interface(
      s"${sfn.capitalize}LT",
      "language" -> TSString,
      sfn        -> TSString
    )
  }

  implicit def schema[SFN <: String](implicit tt: TypeTag[SFN]): Schema[LanguageType[String, SFN]] = {
    val sfn = tt.tpe match {
      case ConstantType(value) if value.value.isInstanceOf[String] =>
        value.value.asInstanceOf[String]
      case nonConstantType =>
        throw new RuntimeException(
          s"Found non-constant type `$nonConstantType` when decoding `LanguageType`. This is a bug."
        )
    }

    Schema(
      SchemaType.SProduct(
        List(
          SProductField[LanguageType[String, SFN], String](
            FieldName("language"),
            Schema(
              SchemaType.SString(),
              description = Some(s"ISO 639-1 code that represents the language used in `$sfn`")
            ),
            k => None
          ),
          SProductField[LanguageType[String, SFN], String](
            FieldName(sfn),
            Schema(
              SchemaType.SString(),
              description = Some(s"A language specific field $sfn.")
            ),
            k => None
          )
        )
      ),
      Some(SName("no.ndla.common.model.LanguageType"))
    )
  }

  def merge[T, SFN <: String](
      existing: Seq[LanguageType[T, SFN]],
      input: Option[LanguageType[T, SFN]]
  ): Seq[LanguageType[T, SFN]] = {
    input match {
      case Some(value) => existing.filterNot(_.language == value.language) :+ value
      case None        => existing
    }
  }

  implicit def encoder[T: Encoder, SFN <: String: Encoder](implicit tt: TypeTag[SFN]): Encoder[LanguageType[T, SFN]] =
    Encoder.instance(lt => {
      val subfield = tt.tpe match {
        case ConstantType(value) if value.value.isInstanceOf[String] => value.value.asInstanceOf[String]
        case nonConstantType =>
          throw new RuntimeException(
            s"Found non-constant type `$nonConstantType` when decoding `LanguageType`. This is a bug."
          )
      }

      JsonObject(
        subfield   -> lt.value.asJson,
        "language" -> lt.language.asJson
      ).asJson
    })

  implicit def decoder[T: Decoder, SFN <: String: Decoder](implicit tt: TypeTag[SFN]): Decoder[LanguageType[T, SFN]] =
    Decoder.instance { cur =>
      for {
        expectedFieldName <- tt.tpe match {
          case ConstantType(value) if value.value.isInstanceOf[String] => Right(value.value.asInstanceOf[String])
          case nonConstantType =>
            Left(
              DecodingFailure(
                s"Found non-constant type `$nonConstantType` when decoding `LanguageType`. This is a bug.",
                Nil
              )
            )
        }
        language <- cur.downField("language").as[String]
        nonLanguage = cur.keys.flatMap(_.find(_ != "language"))
        subfield <- nonLanguage match {
          case Some(sfn) if sfn == expectedFieldName => Right(sfn.asInstanceOf[SFN])
          case _ => Left(DecodingFailure(s"Missing field with name `$expectedFieldName`.", cur.history))
        }
        value <- cur.downField(subfield).as[T]
      } yield LanguageType(value, language)
    }
}

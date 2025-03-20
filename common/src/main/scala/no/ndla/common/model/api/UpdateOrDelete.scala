/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import cats.implicits.catsSyntaxOptionId
import io.circe.{Decoder, Encoder, FailedCursor, Json}
import sttp.tapir.{FieldName, Schema, SchemaType}

import java.util.UUID

/** To handle `null` and `undefined` differently on `PATCH` endpoints
  *
  * Usage:
  * ```
  * implicit val encoder: Encoder[ApiObject] = UpdateOrDelete.filterMarkers(deriveEncoder)
  * implicit val decoder: Decoder[ApiObject] = deriveDecoder
  * ```
  */
sealed trait UpdateOrDelete[+T]
case object Missing                      extends UpdateOrDelete[Nothing]
case object Delete                       extends UpdateOrDelete[Nothing]
final case class UpdateWith[A](value: A) extends UpdateOrDelete[A]

object UpdateOrDelete {
  val schemaName = s"UpdateOrDeleteInnerSchema-${UUID.randomUUID()}"

  def replaceSchema(schema: sttp.apispec.Schema): Option[sttp.apispec.Schema] = {
    val updateOrDeleteSchema = schema.properties.find { case (k, _) =>
      k == UpdateOrDelete.schemaName
    }

    updateOrDeleteSchema match {
      case Some((_, v: sttp.apispec.Schema)) =>
        v
          .copy(
            title = schema.title,
            description = schema.description,
            deprecated = schema.deprecated
          )
          .nullable
          .some
      case _ => None
    }
  }

  implicit def schema[T](implicit subschema: Schema[T]): Schema[UpdateOrDelete[T]] = {
    val st: SchemaType.SProduct[UpdateOrDelete[T]] = SchemaType.SProduct(
      List(
        SchemaType.SProductField[UpdateOrDelete[T], Any](
          FieldName(schemaName),
          subschema.as,
          _ => throw new RuntimeException("This is a bug")
        )
      )
    )

    subschema.asOption
      .as[UpdateOrDelete[T]]
      .copy(schemaType = st)
  }

  implicit def decodeUpdateOrDelete[A](implicit decodeA: Decoder[A]): Decoder[UpdateOrDelete[A]] =
    Decoder.withReattempt {
      case c: FailedCursor if !c.incorrectFocus => Right(Missing)
      case c =>
        Decoder.decodeOption[A].tryDecode(c).map {
          case Some(a) => UpdateWith(a)
          case None    => Delete
        }
    }

  private[this] val marker: String   = s"$$marker-${UUID.randomUUID()}-marker$$"
  private[this] val markerJson: Json = Json.fromString(marker)

  implicit def encodeUpdateOrDelete[A](implicit encodeA: Encoder[A]): Encoder[UpdateOrDelete[A]] = Encoder.instance {
    case UpdateWith(a) => encodeA(a)
    case Delete        => Json.Null
    case Missing       => markerJson
  }

  def filterMarkers[A](encoder: Encoder.AsObject[A]): Encoder.AsObject[A] =
    encoder.mapJsonObject(
      _.filter { case (_, value) =>
        value != markerJson
      }
    )
}

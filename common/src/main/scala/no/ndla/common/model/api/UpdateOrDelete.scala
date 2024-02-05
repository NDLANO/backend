package no.ndla.common.model.api

import io.circe.{Decoder, Encoder, FailedCursor, Json}
import com.scalatsi.TypescriptType.{TSNull, TSString, TSUndefined, TSUnion}
import com.scalatsi._

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
  implicit def str: TSType[UpdateOrDelete[String]] = {
    TSType.alias[UpdateOrDelete[String]](
      "UpdateOrDeleteString",
      TSUnion(Seq(TSNull, TSUndefined, TSString))
    )
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

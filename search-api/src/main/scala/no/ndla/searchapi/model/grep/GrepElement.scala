/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.grep

import cats.implicits.*
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

sealed trait GrepElement {
  val kode: String
  val status: GrepStatus
  def getTitle: Seq[GrepTitle]
  def getTitleValue(language: String): Option[String] = {
    getTitle.find(title => title.spraak == language).map(title => title.verdi)
  }
}
object GrepElement {
  implicit val decoder: Decoder[GrepElement] =
    List[Decoder[GrepElement]](
      Decoder[GrepKjerneelement].widen,
      Decoder[GrepKompetansemaal].widen,
      Decoder[GrepKompetansemaalSett].widen,
      Decoder[GrepLaererplan].widen,
      Decoder[GrepTverrfagligTema].widen
    ).reduceLeft(_ or _)

  implicit val encoder: Encoder[GrepElement] = Encoder.instance {
    case x: GrepKjerneelement      => x.asJson
    case x: GrepKompetansemaal     => x.asJson
    case x: GrepKompetansemaalSett => x.asJson
    case x: GrepLaererplan         => x.asJson
    case x: GrepTverrfagligTema    => x.asJson
  }
}

sealed trait BelongsToLaerePlan {
  val `tilhoerer-laereplan`: BelongsToObj
}

case class GrepTextObj(tekst: List[GrepTitle])
object GrepTextObj {
  implicit val encoder: Encoder[GrepTextObj] = deriveEncoder
  implicit val decoder: Decoder[GrepTextObj] = deriveDecoder
}

case class GrepKjerneelement(
    kode: String,
    status: GrepStatus,
    tittel: GrepTextObj,
    beskrivelse: GrepTextObj,
    `tilhoerer-laereplan`: BelongsToObj
) extends GrepElement
    with BelongsToLaerePlan {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepKjerneelement {
  implicit val encoder: Encoder[GrepKjerneelement] = deriveEncoder
  implicit val decoder: Decoder[GrepKjerneelement] = deriveDecoder
}

case class BelongsToObj(
    kode: String,
    status: GrepStatus,
    tittel: String
)
object BelongsToObj {
  implicit val encoder: Encoder[BelongsToObj] = deriveEncoder
  implicit val decoder: Decoder[BelongsToObj] = deriveDecoder
}

case class ReferenceObj(
    kode: String,
    status: GrepStatus,
    tittel: String
)
object ReferenceObj {
  implicit val encoder: Encoder[ReferenceObj] = deriveEncoder
  implicit val decoder: Decoder[ReferenceObj] = deriveDecoder
}

case class ReferenceWrapperObj(referanse: ReferenceObj)
object ReferenceWrapperObj {
  implicit val encoder: Encoder[ReferenceWrapperObj] = deriveEncoder
  implicit val decoder: Decoder[ReferenceWrapperObj] = deriveDecoder
}

case class GrepKompetansemaal(
    kode: String,
    status: GrepStatus,
    tittel: GrepTextObj,
    `tilhoerer-laereplan`: BelongsToObj,
    `tilhoerer-kompetansemaalsett`: BelongsToObj,
    `tilknyttede-tverrfaglige-temaer`: List[ReferenceWrapperObj],
    `tilknyttede-kjerneelementer`: List[ReferenceWrapperObj],
    `gjenbruk-av`: Option[ReferenceObj]
) extends GrepElement
    with BelongsToLaerePlan {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepKompetansemaal {
  implicit val encoder: Encoder[GrepKompetansemaal] = deriveEncoder
  implicit val decoder: Decoder[GrepKompetansemaal] = deriveDecoder
}

case class GrepKompetansemaalSett(
    kode: String,
    status: GrepStatus,
    tittel: GrepTextObj,
    `tilhoerer-laereplan`: BelongsToObj,
    kompetansemaal: List[ReferenceObj]
) extends GrepElement
    with BelongsToLaerePlan {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepKompetansemaalSett {
  implicit val encoder: Encoder[GrepKompetansemaalSett] = deriveEncoder
  implicit val decoder: Decoder[GrepKompetansemaalSett] = deriveDecoder
}

case class GrepLaererplan(
    kode: String,
    status: GrepStatus,
    tittel: GrepTextObj,
    `erstattes-av`: List[ReferenceObj]
) extends GrepElement {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepLaererplan {
  implicit val encoder: Encoder[GrepLaererplan] = deriveEncoder
  implicit val decoder: Decoder[GrepLaererplan] = deriveDecoder
}

case class GrepTverrfagligTema(
    kode: String,
    status: GrepStatus,
    tittel: Seq[GrepTitle]
) extends GrepElement {
  override def getTitle: Seq[GrepTitle] = tittel
}
object GrepTverrfagligTema {
  implicit val encoder: Encoder[GrepTverrfagligTema] = deriveEncoder
  implicit val decoder: Decoder[GrepTverrfagligTema] = deriveDecoder
}
class GrepStatus(val value: String) {
  def status: String = value.split("/").lastOption.getOrElse("")
  override def toString: String = status
}
object GrepStatus {
  def apply(value: String): GrepStatus      = new GrepStatus(value)
  def parse(value: String): GrepStatus = {
    new GrepStatus(value)
  }
  implicit val schema: Schema[GrepStatus]            = Schema.string
  implicit val schemaOpt: Schema[Option[GrepStatus]] = Schema.string.asOption

  def fromString(s: String): Option[GrepStatus]            = Option(GrepStatus(s))
  def fromOptString(s: Option[String]): Option[GrepStatus] = s.map(f => new GrepStatus(f))

  implicit val codec: Codec[String, GrepStatus, CodecFormat.TextPlain] = Codec.string.mapDecode { value =>
    DecodeResult.Value(parse(value))
  }(_.status)

  implicit val optCodec: Codec[Option[String], Option[GrepStatus], CodecFormat.TextPlain] = {
    Codec
      .id[Option[String], TextPlain](TextPlain(), Schema.string)
      .mapDecode(x =>
        DecodeResult.Value(
          fromOptString(x)
        )
      )(x => x.map(_.status))
  }

  implicit val encoder: Encoder[GrepStatus] = Encoder.instance { gs => gs.toString.asJson }
  implicit val decoder: Decoder[GrepStatus] = (c: HCursor) =>
    c.as[String].flatMap { str =>
      fromString(str) match {
        case Some(value) => Right(value)
        case None        => Left(DecodingFailure(DecodingFailure.Reason.CustomReason("Not a string"), c))
      }
    }
}

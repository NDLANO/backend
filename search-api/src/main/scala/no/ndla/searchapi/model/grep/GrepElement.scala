/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.grep

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait GrepElement {
  val kode: String
  def getTitle: Seq[GrepTitle]
}

sealed trait BelongsToLaerePlan {
  val `tilhoerer-laereplan`: BelongsToObj
}

case class TitleObj(tekst: List[GrepTitle])
object TitleObj {
  implicit val encoder: Encoder[TitleObj] = deriveEncoder
  implicit val decoder: Decoder[TitleObj] = deriveDecoder
}

case class GrepKjerneelement(kode: String, tittel: TitleObj, `tilhoerer-laereplan`: BelongsToObj)
    extends GrepElement
    with BelongsToLaerePlan {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepKjerneelement {
  implicit val encoder: Encoder[GrepKjerneelement] = deriveEncoder
  implicit val decoder: Decoder[GrepKjerneelement] = deriveDecoder
}

case class BelongsToObj(kode: String)
object BelongsToObj {
  implicit val encoder: Encoder[BelongsToObj] = deriveEncoder
  implicit val decoder: Decoder[BelongsToObj] = deriveDecoder
}

case class GrepKompetansemaal(kode: String, tittel: TitleObj, `tilhoerer-laereplan`: BelongsToObj)
    extends GrepElement
    with BelongsToLaerePlan {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepKompetansemaal {
  implicit val encoder: Encoder[GrepKompetansemaal] = deriveEncoder
  implicit val decoder: Decoder[GrepKompetansemaal] = deriveDecoder
}

case class GrepKompetansemaalSett(kode: String, tittel: TitleObj, `tilhoerer-laereplan`: BelongsToObj)
    extends GrepElement
    with BelongsToLaerePlan {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepKompetansemaalSett {
  implicit val encoder: Encoder[GrepKompetansemaalSett] = deriveEncoder
  implicit val decoder: Decoder[GrepKompetansemaalSett] = deriveDecoder
}

case class GrepLaererplan(kode: String, tittel: TitleObj) extends GrepElement {
  override def getTitle: Seq[GrepTitle] = tittel.tekst
}
object GrepLaererplan {
  implicit val encoder: Encoder[GrepLaererplan] = deriveEncoder
  implicit val decoder: Decoder[GrepLaererplan] = deriveDecoder
}

case class GrepTverrfagligTema(kode: String, tittel: Seq[GrepTitle]) extends GrepElement {
  override def getTitle: Seq[GrepTitle] = tittel
}
object GrepTverrfagligTema {
  implicit val encoder: Encoder[GrepTverrfagligTema] = deriveEncoder
  implicit val decoder: Decoder[GrepTverrfagligTema] = deriveDecoder
}

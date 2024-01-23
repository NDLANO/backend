/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.implicits.toFunctorOps
import com.scalatsi.{TSIType, TSNamedType, TSType}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

@description("The Menu object")
case class Menu(
    @description("Id of the article") articleId: Long,
    @description("List of submenu objects") menu: List[MenuData],
    @description("Hide submenu") hideLevel: Boolean
) extends MenuData

@description("Object containing frontpage data")
case class FrontPage(
    @description("Id of the frontpage") articleId: Long,
    @description("List of Menu objects") menu: List[Menu]
)

object Menu {
  implicit val encodeMenu: Encoder[Menu] = deriveEncoder
  implicit val decodeMenu: Decoder[Menu] = deriveDecoder

  implicit val encodeMenuData: Encoder[MenuData] = Encoder.instance { case menu: Menu => menu.asJson }
  implicit val decodeMenuData: Decoder[MenuData] = Decoder[Menu].widen

  implicit val frontPageEncoder: Encoder[FrontPage] = deriveEncoder
  implicit val frontPageDecoder: Decoder[FrontPage] = deriveDecoder

  implicit val menuTSI: TSIType[Menu] = {
    @unused
    implicit val menuData: TSNamedType[MenuData] = TSType.external[MenuData]("IMenuData")
    TSType.fromCaseClass[Menu]
  }
}

sealed trait MenuData {}
object MenuData {
  def apply(articleId: Long, menu: List[MenuData], hideLevel: Boolean): MenuData = new Menu(articleId, menu, hideLevel)

  implicit val menuDataAlias: TSNamedType[MenuData] = TSType.alias[MenuData]("IMenuData", Menu.menuTSI.get)
}

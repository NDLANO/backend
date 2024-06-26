/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.api

import cats.implicits.toFunctorOps
import com.scalatsi.{TSIType, TSNamedType, TSType}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

@description("The Menu object")
case class Menu(
    @description("Id of the article") articleId: Long,
    @description("List of submenu objects") menu: List[MenuData],
    @description("Hide this level in menu") hideLevel: Option[Boolean]
) extends MenuData

@description("Object containing frontpage data")
case class FrontPage(
    @description("Id of the frontpage") articleId: Long,
    @description("List of Menu objects") menu: List[Menu]
)

object FrontPage {
  implicit val encodeFrontPage: Encoder[FrontPage] = deriveEncoder
  implicit val decodeFrontPage: Decoder[FrontPage] = deriveDecoder
}

object Menu {
  implicit val encodeMenu: Encoder[Menu] = deriveEncoder
  implicit val decodeMenu: Decoder[Menu] = deriveDecoder

  implicit val encodeMenuData: Encoder[MenuData] = Encoder.instance { case menu: Menu => menu.asJson }
  implicit val decodeMenuData: Decoder[MenuData] = Decoder[Menu].widen

  implicit val menuTSI: TSIType[Menu] = {
    @unused
    implicit val menuData: TSNamedType[MenuData] = TSType.external[MenuData]("IMenuData")
    TSType.fromCaseClass[Menu]
  }
}

sealed trait MenuData {}
object MenuData {
  implicit val menuDataAlias: TSNamedType[MenuData] = TSType.alias[MenuData]("IMenuData", Menu.menuTSI.get)
}

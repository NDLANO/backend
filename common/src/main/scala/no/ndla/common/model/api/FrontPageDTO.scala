/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import cats.implicits.toFunctorOps
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("The Menu object")
case class MenuDTO(
    @description("Id of the article") articleId: Long,
    @description("List of submenu objects") menu: List[MenuDataDTO],
    @description("Hide this level in menu") hideLevel: Option[Boolean]
) extends MenuDataDTO

@description("Object containing frontpage data")
case class FrontPageDTO(
    @description("Id of the frontpage") articleId: Long,
    @description("List of Menu objects") menu: List[MenuDTO]
)

object FrontPageDTO {
  implicit val encodeFrontPage: Encoder[FrontPageDTO] = deriveEncoder
  implicit val decodeFrontPage: Decoder[FrontPageDTO] = deriveDecoder
}

object MenuDTO {
  implicit val encodeMenu: Encoder[MenuDTO] = deriveEncoder
  implicit val decodeMenu: Decoder[MenuDTO] = deriveDecoder

  implicit val encodeMenuData: Encoder[MenuDataDTO] = Encoder.instance { case menu: MenuDTO => menu.asJson }
  implicit val decodeMenuData: Decoder[MenuDataDTO] = Decoder[MenuDTO].widen
}

sealed trait MenuDataDTO {}

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
import sttp.tapir.Schema
import sttp.tapir.SchemaType.SProduct
import sttp.tapir.FieldName
import sttp.tapir.SchemaType
import no.ndla.common.DeriveHelpers

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

  import sttp.tapir.generic.auto.*
  implicit def schema: Schema[MenuDTO] = DeriveHelpers.getSchema
}

sealed trait MenuDataDTO {}

object MenuDataDTO {
  implicit def schema: Schema[MenuDataDTO] = MenuDTO.schema.as
}

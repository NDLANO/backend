/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import com.scalatsi.{TSIType, TSNamedType, TSType}
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

@description("The Menu object")
case class Menu(
    @description("Id of the article") articleId: Long,
    @description("List of submenu objects") menu: List[Menu]
)

@description("Object containing frontpage data")
case class FrontPage(
    @description("Id of the frontpage") articleId: Long,
    @description("List of Menu objects") menu: List[Menu]
)
//  extends FrontPageData
//
//object FrontPage {
//  implicit val frontPageSI: TSIType[FrontPage] = {
//    @unused
//    implicit val frontPageData: TSNamedType[FrontPageData] = TSType.external[FrontPageData]("IFrontPageData")
//    TSType.fromCaseClass[FrontPage]
//  }
//}
//
//sealed trait FrontPageData {}
//object FrontPageData {
//  def apply(
//      articleId: Long,
//      menu: List[Menu]
//  ): FrontPageData = {
//    FrontPage(
//      articleId,
//      menu
//    )
//  }
//  implicit val frontPageDataAlias: TSNamedType[FrontPageData] =
//    TSType.alias[FrontPageData]("IFrontPageData", FrontPage.frontPageSI.get)
//}

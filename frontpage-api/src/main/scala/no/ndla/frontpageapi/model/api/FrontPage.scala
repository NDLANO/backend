/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import sttp.tapir.Schema.annotations.description

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

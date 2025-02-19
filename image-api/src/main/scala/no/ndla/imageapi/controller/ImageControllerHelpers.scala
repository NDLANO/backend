/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import sttp.tapir._

trait ImageControllerHelpers {
  this: Props =>
  object ImageControllerHelpers {
    val pageNo: EndpointInput.Query[Int] =
      query[Int]("page")
        .description("The page number of the search hits to display.")
        .default(1)

    val pageSize: EndpointInput.Query[Int] =
      query[Int]("page-size")
        .description("The number of search hits to display for each page.")
        .default(props.DefaultPageSize)
  }
}

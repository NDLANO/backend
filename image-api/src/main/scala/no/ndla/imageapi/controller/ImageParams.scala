/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import sttp.tapir.EndpointIO.annotations.*

// format: off
case class ImageParams(
  @header("app-key")
  @description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access.")
  appKey: Option[String],
  @query
  @description("The target width to resize the image (the unit is pixles). Image proportions are kept intact")
  width: Option[Double],
  @query
  @description("The target height to resize the image (the unit is pixles). Image proportions are kept intact")
  height: Option[Double],
  @query
  @description("The first image coordinate X, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop start position. If used the other crop parameters must also be supplied")
  cropStartX: Option[Double],
  @query
  @description("The first image coordinate Y, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop start position. If used the other crop parameters must also be supplied")
  cropStartY: Option[Double],
  @query
  @description("The end image coordinate X, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop end position. If used the other crop parameters must also be supplied")
  cropEndX: Option[Double],
  @query
  @description("The end image coordinate Y, in percent (0 to 100) or pixels depending on cropUnit, specifying the crop end position. If used the other crop parameters must also be supplied")
  cropEndY: Option[Double],
  @query
  @description("The unit of the crop parameters. Can be either 'percent' or 'pixel'. If omitted the unit is assumed to be 'percent'")
  cropUnit: Option[String],
  @query
  @description("The end image coordinate X, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied")
  focalX: Option[Double],
  @query
  @description("The end image coordinate Y, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied")
  focalY: Option[Double],
  @query
  @description("The wanted aspect ratio, defined as width/height. To be used together with the focal parameters. If used the width and height is ignored and derived from the aspect ratio instead.")
  ratio: Option[Double],
  @query
  @description("The wanted aspect ratio, defined as width/height. To be used together with the focal parameters. If used the width and height is ignored and derived from the aspect ratio instead.")
  language: Option[String]
)
// format: on

object ImageParams {
  def empty: ImageParams = ImageParams(
    appKey = None,
    width = None,
    height = None,
    cropStartX = None,
    cropStartY = None,
    cropEndX = None,
    cropEndY = None,
    cropUnit = None,
    focalX = None,
    focalY = None,
    ratio = None,
    language = None
  )
}

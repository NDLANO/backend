/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import sttp.model.{HeaderNames, StatusCode}
import sttp.tapir.*

import java.io.InputStream

case class ImageResponse(inputStream: InputStream, contentType: String, contentLength: String)

object ImageResponse {
  def endpointOutput(using props: Props): EndpointOutput[ImageResponse] = statusCode(StatusCode.Ok)
    .and(inputStreamBody)
    .and(header[String](HeaderNames.ContentType))
    .and(header[String](HeaderNames.ContentLength))
    .and(header(HeaderNames.CacheControl, props.RawControllerCacheControl))
    .mapTo[ImageResponse]
}

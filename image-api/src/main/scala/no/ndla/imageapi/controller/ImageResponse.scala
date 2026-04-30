/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import sttp.model.{HeaderNames, StatusCode}
import sttp.tapir.*

import java.io.InputStream

sealed trait ImageResponse

object ImageResponse {
  case class Stream(
      inputStream: InputStream,
      contentType: String,
      contentLength: String,
      contentDisposition: Option[String],
  ) extends ImageResponse

  case class Redirect(url: String) extends ImageResponse

  val endpointOutput: EndpointOutput.OneOf[ImageResponse, ImageResponse] = oneOf(
    oneOfVariant(
      statusCode(StatusCode.Ok)
        .and(inputStreamBody)
        .and(header[String](HeaderNames.ContentType))
        .and(header[String](HeaderNames.ContentLength))
        .and(header[Option[String]](HeaderNames.ContentDisposition))
        .mapTo[Stream]
    ),
    oneOfVariant(statusCode(StatusCode.Found).and(header[String](HeaderNames.Location)).mapTo[Redirect]),
  )
}

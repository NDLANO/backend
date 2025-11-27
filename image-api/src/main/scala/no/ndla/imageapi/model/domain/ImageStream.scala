/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import java.io.InputStream

enum ImageStream(val stream: InputStream, val fileName: String, val contentLength: Long, val contentType: String) {
  case Processable(
      override val stream: InputStream,
      override val fileName: String,
      override val contentLength: Long,
      format: ProcessableImageFormat,
  ) extends ImageStream(stream, fileName, contentLength, format.toContentType)

  case Gif(override val stream: InputStream, override val fileName: String, override val contentLength: Long)
      extends ImageStream(stream, fileName, contentLength, "image/gif")

  case Unprocessable(
      override val stream: InputStream,
      override val fileName: String,
      override val contentLength: Long,
      override val contentType: String,
  ) extends ImageStream(stream, fileName, contentLength, contentType)
}

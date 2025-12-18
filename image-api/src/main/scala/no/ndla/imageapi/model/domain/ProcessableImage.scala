/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImageWriter
import no.ndla.imageapi.service.ScrimageUtil

import scala.util.Try

case class ProcessableImage(image: ImmutableImage, fileName: String, format: ProcessableImageFormat) {
  def toProcessableImageStream: Try[ImageStream.Processable] = ScrimageUtil.imageToProcessableStream(this)

  def toProcessableStreamWithWriter(writer: ImageWriter, format: ProcessableImageFormat): Try[ImageStream.Processable] =
    ScrimageUtil.imageToStreamWithWriter(this, writer, format)

  def transform(f: ImmutableImage => ImmutableImage): Try[ProcessableImage] = ScrimageUtil.transformImage(this, f)
}

object ProcessableImage {
  def fromStream(stream: ImageStream.Processable): Try[ProcessableImage] = ScrimageUtil.imageFromStream(stream)
}

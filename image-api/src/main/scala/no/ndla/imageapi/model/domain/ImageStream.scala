/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.{JpegWriter, PngWriter}
import com.sksamuel.scrimage.webp.WebpWriter

import java.io.{ByteArrayInputStream, InputStream}
import scala.util.Try

sealed trait ImageStream {
  def toStream: InputStream
  def fileName: String
  def contentType: String
}

final case class ProcessableImageStream(
    image: ImmutableImage,
    override val fileName: String,
    format: ProcessableImageFormat,
) extends ImageStream {
  override def toStream: InputStream = {
    val writer = format match {
      case ProcessableImageFormat.Jpeg => JpegWriter.Default
      case ProcessableImageFormat.Png  => PngWriter.MaxCompression
      case ProcessableImageFormat.Webp => WebpWriter.DEFAULT
    }
    val bytes = image.bytes(writer)
    new ByteArrayInputStream(bytes)
  }

  override val contentType: String = format.toContentType

  def transform(f: ImmutableImage => ImmutableImage): Try[ProcessableImageStream] =
    Try(f(image)).map(transformed => copy(image = transformed))
}

final case class UnprocessableImageStream(
    imageBytes: Array[Byte],
    override val fileName: String,
    override val contentType: String,
) extends ImageStream {
  override def toStream: InputStream = new ByteArrayInputStream(imageBytes)
}

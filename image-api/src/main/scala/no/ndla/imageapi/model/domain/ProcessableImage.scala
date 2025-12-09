/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.*
import com.sksamuel.scrimage.webp.{WebpImageReader, WebpWriter}

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import scala.jdk.CollectionConverters.*
import scala.util.{Success, Try, Using}

case class ProcessableImage(image: ImmutableImage, fileName: String, format: ProcessableImageFormat) {
  def toProcessableImageStream: Try[ImageStream.Processable] = {
    val writer = format match {
      case ProcessableImageFormat.Jpeg => JpegWriter.Default
      case ProcessableImageFormat.Png  => PngWriter.MaxCompression
      case ProcessableImageFormat.Webp => WebpWriter.DEFAULT
    }

    Try(image.bytes(writer)).map(bytes =>
      ImageStream.Processable(new ByteArrayInputStream(bytes), fileName, bytes.length, format)
    )
  }

  def transform(f: ImmutableImage => ImmutableImage): Try[ProcessableImage] =
    Try(f(image)).map(ProcessableImage(_, fileName, format))
}

object ProcessableImage {
  private val readers                      = Seq(new ImageIOReader, new PngReader, new WebpImageReader)
  private val loader: ImmutableImageLoader = ImmutableImage.loader().withImageReaders(readers.asJava)

  def fromStream(stream: ImageStream.Processable): Try[ProcessableImage] = Using(stream) { imageStream =>
    for {
      image              <- Try(loader.fromStream(imageStream.stream))
      imageWithFixedType <- fixImageUnderlyingType(image)
    } yield ProcessableImage(imageWithFixedType, imageStream.fileName, imageStream.format)
  }.flatten
}

// Due to a bug in Scrimage, 16-bit grayscale images must be converted to e.g., 8-bit RGBA
// See https://github.com/dbcxy/java-image-scaling/issues/35, which is used internally by Scrimage
private def fixImageUnderlyingType(image: ImmutableImage): Try[ImmutableImage] = image.getType match {
  case BufferedImage.TYPE_USHORT_GRAY => Try(image.copy(ImmutableImage.DEFAULT_DATA_TYPE))
  case _                              => Success(image)
}

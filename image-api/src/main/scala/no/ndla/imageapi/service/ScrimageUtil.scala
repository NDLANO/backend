/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.sksamuel.scrimage.format.{Format, FormatDetector}
import com.sksamuel.scrimage.nio.*
import com.sksamuel.scrimage.webp.{WebpImageReader, WebpWriter}
import com.sksamuel.scrimage.{ImageParseException, ImmutableImage}
import no.ndla.common.TryUtil
import no.ndla.common.TryUtil.throwIfInterrupted
import no.ndla.imageapi.model.domain.{ImageDimensions, ImageStream, ProcessableImage, ProcessableImageFormat}

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, InputStream}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.{Failure, Success, Try, Using}

object ScrimageUtil {
  // NOTE: We rely on the presence of WebpImageReader to handle exceptions from interrupts. The order is important, and
  // we want WebpImageReader last in order to properly catch InterruptedExceptions.
  private val readers                      = Seq(new ImageIOReader, new PngReader, new WebpImageReader)
  private val loader: ImmutableImageLoader = ImmutableImage.loader().withImageReaders(readers.asJava)

  def detectFormatFromInputStream(stream: InputStream): Try[Option[Format]] =
    Try.throwIfInterrupted(FormatDetector.detect(stream).toScala)

  def imageFromStream(stream: ImageStream.Processable): Try[ProcessableImage] = {
    val maybeImage = Using(stream) { imageStream =>
      for {
        image              <- Try.throwIfInterrupted(loader.fromStream(imageStream.stream))
        imageWithFixedType <- fixImageUnderlyingType(image)
      } yield ProcessableImage(imageWithFixedType, imageStream.fileName, imageStream.format)
    }.flatten

    maybeImage match {
      case Success(i)                       => Success(i)
      case Failure(ex: ImageParseException) =>
        // WebpImageReader should have thrown an exception with cause of InterruptedException if thread was interrupted,
        // and will be in the list of errors of the ImageParseException.
        ex.getErrors
          .asScala
          .find(TryUtil.containsInterruptedException)
          .foreach(e => TryUtil.rethrowInterruptedWithSuppressed(Some(e)))
        Failure(ex)
      case Failure(ex) => Failure(ex)
    }
  }

  def imageToProcessableStream(processableImage: ProcessableImage): Try[ImageStream.Processable] = {
    val writer = processableImage.format match {
      case ProcessableImageFormat.Jpeg => JpegWriter.Default
      case ProcessableImageFormat.Png  => PngWriter.MaxCompression
      case ProcessableImageFormat.Webp => WebpWriter.DEFAULT
    }

    imageToStreamWithWriter(processableImage, writer, processableImage.format)
  }

  def imageToStreamWithWriter(
      processableImage: ProcessableImage,
      writer: ImageWriter,
      writerFormat: ProcessableImageFormat,
  ): Try[ImageStream.Processable] = Try
    .throwIfInterrupted(processableImage.image.bytes(writer))
    .map(bytes =>
      ImageStream.Processable(
        new ByteArrayInputStream(bytes),
        processableImage.fileName,
        bytes.length.toLong,
        writerFormat,
      )
    )

  def getDimensionsFromGifStream(stream: ImageStream.Gif): Try[(ImageStream.Gif, ImageDimensions)] = Try
    .throwIfInterrupted {
      Using.resource(stream) { imageStream =>
        val bytes                   = imageStream.stream.readAllBytes()
        val gif                     = AnimatedGifReader.read(ImageSource.of(bytes))
        val awtDim                  = gif.getDimensions
        val dim                     = ImageDimensions(awtDim.width, awtDim.height)
        val byteStream              = new ByteArrayInputStream(bytes)
        val stream: ImageStream.Gif = ImageStream.Gif(byteStream, imageStream.fileName, bytes.length)
        (stream, dim)
      }
    }

  def transformImage(processableImage: ProcessableImage, f: ImmutableImage => ImmutableImage): Try[ProcessableImage] =
    Try.throwIfInterrupted(f(processableImage.image)).map(i => processableImage.copy(image = i))

  // Due to a bug in Scrimage, 16-bit grayscale images must be converted to e.g., 8-bit RGBA
  // See https://github.com/dbcxy/java-image-scaling/issues/35, which is used internally by Scrimage
  private def fixImageUnderlyingType(image: ImmutableImage): Try[ImmutableImage] = image.getType match {
    case BufferedImage.TYPE_USHORT_GRAY => Try(image.copy(ImmutableImage.DEFAULT_DATA_TYPE))
    case _                              => Success(image)
  }
}

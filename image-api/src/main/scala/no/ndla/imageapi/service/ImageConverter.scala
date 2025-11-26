/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.{ImmutableImage, ScaleMethod}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.aws.NdlaS3Object
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.implicits.toTry
import no.ndla.common.model.domain.UploadedFile
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.ImageUnprocessableFormatException
import no.ndla.imageapi.model.domain.*

import java.awt.image.BufferedImage
import java.io.{BufferedInputStream, InputStream}
import java.lang.Math.{abs, max, min}
import scala.jdk.OptionConverters.*
import scala.util.{Failure, Success, Try}

case class PixelPoint(x: Int, y: Int) // A point given with pixles
case class PercentPoint(
    x: Double,
    y: Double,
) { // A point given with values from MinValue to MaxValue. MinValue,MinValue is top-left, MaxValue,MaxValue is bottom-right
  import PercentPoint.*
  if (!inRange(x) || !inRange(y)) throw new ValidationException(errors =
    Seq(ValidationMessage("PercentPoint", s"Invalid value for a PixelPoint. Must be in range $MinValue-$MaxValue"))
  )

  lazy val normalizedX: Double = normalise(x)
  lazy val normalizedY: Double = normalise(y)
}

object PercentPoint {
  val MinValue: Int = 0
  val MaxValue: Int = 100

  private def inRange(n: Double): Boolean      = n >= MinValue && n <= MaxValue
  private def normalise(coord: Double): Double = coord / MaxValue.toDouble
}

class ImageConverter(using props: Props) extends StrictLogging {
  private val nonResizableMimeTypes = List("image/gif", "image/svg", "image/svg+xml")

  def s3ObjectToImageStream(s3Object: NdlaS3Object): Try[ImageStream] =
    inputStreamToImageStream(s3Object.stream, s3Object.key, s3Object.contentLength, s3Object.contentType)

  def uploadedFileToImageStream(file: UploadedFile, fileName: String): Try[ImageStream] =
    inputStreamToImageStream(file.stream, fileName, file.fileSize, file.contentType.getOrElse(""))

  private def inputStreamToImageStream(
      inputStream: InputStream,
      fileName: String,
      contentLength: Long,
      contentType: String,
  ): Try[ImageStream] = {
    // Use buffered stream with mark to avoid creating multiple streams
    val stream = new BufferedInputStream(inputStream)
    stream.mark(32)

    val format = (
      for {
        maybeFormat <- Try(FormatDetector.detect(stream).toScala)
        _           <- Try(stream.reset())
        format      <- maybeFormat
          .flatMap(ProcessableImageFormat.fromScrimageFormat)
          .toTry(ImageUnprocessableFormatException(contentType))
      } yield format
    ) match {
      case Success(f)                                                                                             => f
      case Failure(ImageUnprocessableFormatException(contentType)) if nonResizableMimeTypes.contains(contentType) =>
        return Try(stream.readAllBytes()).map(bytes =>
          UnprocessableImageStream(bytes, fileName, contentLength, contentType)
        )
      case Failure(ex) => return Failure(ex)
    }

    for {
      image              <- Try(ImmutableImage.loader().fromStream(stream))
      imageWithFixedType <- fixImageUnderlyingType(image)
      imageStream         = ProcessableImageStream(imageWithFixedType, fileName, contentLength, format)
    } yield imageStream
  }

  // Due to a bug in Scrimage, 16-bit grayscale images must be converted to e.g., 8-bit RGBA
  // See https://github.com/dbcxy/java-image-scaling/issues/35, which is used internally by Scrimage
  private def fixImageUnderlyingType(image: ImmutableImage): Try[ImmutableImage] = image.getType match {
    case BufferedImage.TYPE_USHORT_GRAY => Try(image.copy(ImmutableImage.DEFAULT_DATA_TYPE))
    case _                              => Success(image)
  }

  private def scaleMethodFor(targetSize: Int): ScaleMethod =
    if (targetSize >= props.ImageScalingUltraMinSize && targetSize <= props.ImageScalingUltraMaxSize)
      ScaleMethod.Lanczos3
    else ScaleMethod.Bicubic

  def resizeToVariantSize(original: ImmutableImage, variant: ImageVariantSize): Try[ImmutableImage] = Try {
    // If the image is to be resized to exactly the same width as itself, Scrimage doesn't return a new copy.
    // This causes issues when the original image is reused in generating other variants, so we create a copy ourselves
    if (original.width == variant.width) original.copy()
    else original.scaleToWidth(variant.width)
  }

  def resize(
      originalImage: ProcessableImageStream,
      targetWidth: Int,
      targetHeight: Int,
  ): Try[ProcessableImageStream] = {
    val img        = originalImage.image
    val targetSize = min(min(img.width, targetWidth), min(img.height, targetHeight))
    val method     = scaleMethodFor(targetSize)
    originalImage.transform(_.bound(targetWidth, targetHeight, method))
  }

  def resizeWidth(originalImage: ProcessableImageStream, size: Int): Try[ProcessableImageStream] = {
    val targetSize = Math.min(size, originalImage.image.width)
    val method     = scaleMethodFor(targetSize)
    originalImage.transform(_.scaleToWidth(targetSize, method))
  }

  def resizeHeight(originalImage: ProcessableImageStream, size: Int): Try[ProcessableImageStream] = {
    val targetSize = Math.min(size, originalImage.image.height)
    val method     = scaleMethodFor(targetSize)
    originalImage.transform(_.scaleToHeight(targetSize, method))
  }

  def crop(
      originalImage: ProcessableImageStream,
      topLeft: PixelPoint,
      bottomRight: PixelPoint,
  ): Try[ProcessableImageStream] = {
    val (width, height) = getWidthHeight(topLeft, bottomRight, originalImage.image.width, originalImage.image.height)

    if (width <= 0 || height <= 0) Success(originalImage)
    else originalImage.transform(_.subimage(topLeft.x, topLeft.y, width, height))
  }

  def crop(
      originalImage: ProcessableImageStream,
      start: PercentPoint,
      end: PercentPoint,
  ): Try[ProcessableImageStream] = {
    val (topLeft, bottomRight) = transformCoordinates(originalImage.image.width, originalImage.image.height, start, end)
    crop(originalImage, topLeft, bottomRight)
  }

  private def getStartEndCoords(focalPoint: Int, targetDimensionSize: Int, originalDimensionSize: Int): (Int, Int) = {
    val ts                 = min(targetDimensionSize.toDouble, originalDimensionSize.toDouble) / 2.0
    val (start, end)       = (focalPoint - ts.floor.toInt, focalPoint + ts.round.toInt)
    val (startRem, endRem) = (abs(min(start, 0)), max(end - originalDimensionSize, 0))

    (max(start - endRem, 0), min(end + startRem, originalDimensionSize))
  }

  def dynamicCrop(
      originalImage: ProcessableImageStream,
      percentFocalPoint: PercentPoint,
      targetWidthOpt: Option[Int],
      targetHeightOpt: Option[Int],
      ratioOpt: Option[Double],
  ): Try[ProcessableImageStream] = {
    val img                       = originalImage.image
    val focalPoint                = toPixelPoint(percentFocalPoint, img.width, img.height)
    val (imageWidth, imageHeight) = (img.width, img.height)

    val (targetWidth: Int, targetHeight: Int) = (targetWidthOpt, targetHeightOpt, ratioOpt) match {
      case (_, _, Some(ratio))   => minimalCropSizesToPreserveRatio(imageWidth, imageHeight, ratio)
      case (None, None, _)       => return Success(originalImage)
      case (Some(w), Some(h), _) => (min(w, imageWidth), min(h, imageHeight))
      case (Some(w), None, _)    =>
        val actualTargetWidth             = min(imageWidth, w)
        val widthReductionPercent: Double = actualTargetWidth.toDouble / imageWidth.toDouble
        (
          w,
          (
            imageHeight * widthReductionPercent
          ).toInt,
        )
      case (None, Some(h), _) =>
        val actualTargetHeight             = min(imageHeight, h)
        val heightReductionPercent: Double = actualTargetHeight.toDouble / imageHeight.toDouble
        (
          (
            imageWidth * heightReductionPercent
          ).toInt,
          actualTargetHeight,
        )
    }

    val (startY, endY) = getStartEndCoords(focalPoint.y, targetHeight, imageHeight)
    val (startX, endX) = getStartEndCoords(focalPoint.x, targetWidth, imageWidth)

    crop(originalImage, PixelPoint(startX, startY), PixelPoint(endX, endY))
  }

  def minimalCropSizesToPreserveRatio(imageWidth: Int, imageHeight: Int, ratio: Double): (Int, Int) = {
    val newHeight = Math.min(imageWidth / ratio, imageHeight.toDouble).toInt
    val newWidth  = Math.min(newHeight * ratio, imageWidth.toDouble).toInt
    (newWidth, newHeight)
  }

  // Given two sets of coordinates; reorganize them so that the first coordinate is the top-left,
  // and the other coordinate is the bottom-right
  private[service] def transformCoordinates(
      imageWidth: Int,
      imageHeight: Int,
      start: PercentPoint,
      end: PercentPoint,
  ): (PixelPoint, PixelPoint) = {
    val topLeft     = PercentPoint(min(start.x, end.x), min(start.y, end.y))
    val bottomRight = PercentPoint(max(start.x, end.x), max(start.y, end.y))

    (toPixelPoint(topLeft, imageWidth, imageHeight), toPixelPoint(bottomRight, imageWidth, imageHeight))
  }

  private def toPixelPoint(point: PercentPoint, width: Int, height: Int) = {
    PixelPoint(
      (
        point.normalizedX * width
      ).toInt,
      (
        point.normalizedY * height
      ).toInt,
    )
  }

  private[service] def getWidthHeight(
      start: PixelPoint,
      end: PixelPoint,
      imageWidth: Int,
      imageHeight: Int,
  ): (Int, Int) = {
    val width  = abs(start.x - end.x)
    val height = abs(start.y - end.y)
    (min(width, imageWidth - start.x), min(height, imageHeight - start.y))
  }
}

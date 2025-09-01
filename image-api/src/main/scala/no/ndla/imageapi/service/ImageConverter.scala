/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.lang.Math.{abs, max, min}
import javax.imageio.ImageIO
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.ImageStream
import org.imgscalr.Scalr
import org.imgscalr.Scalr.Mode

import java.awt.{Color, Transparency}
import scala.util.{Success, Try}

case class PixelPoint(x: Int, y: Int) // A point given with pixles
case class PercentPoint(x: Double, y: Double) { // A point given with values from MinValue to MaxValue. MinValue,MinValue is top-left, MaxValue,MaxValue is bottom-right
  import PercentPoint.*
  if (!inRange(x) || !inRange(y))
    throw new ValidationException(
      errors = Seq(
        ValidationMessage("PercentPoint", s"Invalid value for a PixelPoint. Must be in range $MinValue-$MaxValue")
      )
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

class ImageConverter(using
    props: Props
) extends StrictLogging {

  /** This method adds a white background to a [[BufferedImage]], useful for removing transparent pixels for image types
    * that doesn't support transparency
    */
  private def fillTransparentPixels(image: BufferedImage): BufferedImage = {
    val width    = image.getWidth()
    val height   = image.getHeight()
    val newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g        = newImage.createGraphics()
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, width, height)
    g.drawRenderedImage(image, null)
    g.dispose()
    newImage
  }

  private[service] def toImageStream(bufferedImage: BufferedImage, originalImage: ImageStream): ImageStream = {
    val outputStream      = new ByteArrayOutputStream()
    val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
    val writerIter        = ImageIO.getImageWritersByMIMEType(originalImage.contentType)

    val onlyOpaqueTypes          = Seq("image/jpeg", "image/jpg")
    val shouldRemoveTransparency = onlyOpaqueTypes.contains(originalImage.contentType) &&
      bufferedImage.getColorModel.getTransparency != Transparency.OPAQUE

    val newImage = if (shouldRemoveTransparency) fillTransparentPixels(bufferedImage) else bufferedImage

    if (writerIter.hasNext) {
      val writer = writerIter.next
      writer.setOutput(imageOutputStream)
      writer.write(newImage)
    } else {
      logger.warn(
        s"Writer for content-type ${originalImage.contentType} not found, using ${originalImage.format} as format"
      )
      ImageIO.write(newImage, originalImage.format, imageOutputStream)
    }

    new ImageStream {
      override def stream: ByteArrayInputStream    = new ByteArrayInputStream(outputStream.toByteArray)
      override def contentType: String             = originalImage.contentType
      override def fileName: String                = originalImage.fileName
      override lazy val sourceImage: BufferedImage = ImageIO.read(stream)
    }
  }

  def resize(originalImage: ImageStream, targetWidth: Int, targetHeight: Int): Try[ImageStream] = {
    val sourceImage = originalImage.sourceImage
    val minWidth    = min(targetWidth, sourceImage.getWidth)
    val minHeight   = min(targetHeight, sourceImage.getHeight)
    val method      =
      if (minWidth >= props.ImageScalingUltraMinSize && minWidth <= props.ImageScalingUltraMaxSize)
        Scalr.Method.ULTRA_QUALITY
      else Scalr.Method.AUTOMATIC
    Try(Scalr.resize(sourceImage, method, minWidth, minHeight))
      .map(resized => toImageStream(resized, originalImage))
  }

  private def resize(originalImage: ImageStream, mode: Mode, targetSize: Int): Try[ImageStream] = {
    val sourceImage = originalImage.sourceImage
    val method      =
      if (targetSize >= props.ImageScalingUltraMinSize && targetSize <= props.ImageScalingUltraMaxSize)
        Scalr.Method.ULTRA_QUALITY
      else Scalr.Method.AUTOMATIC
    Try(Scalr.resize(sourceImage, method, mode, targetSize)).map(resized => toImageStream(resized, originalImage))
  }

  def resizeWidth(originalImage: ImageStream, size: Int): Try[ImageStream] =
    resize(originalImage, Mode.FIT_TO_WIDTH, Math.min(size, originalImage.sourceImage.getWidth))

  def resizeHeight(originalImage: ImageStream, size: Int): Try[ImageStream] =
    resize(originalImage, Mode.FIT_TO_HEIGHT, Math.min(size, originalImage.sourceImage.getHeight))

  private def crop(
      image: ImageStream,
      sourceImage: BufferedImage,
      topLeft: PixelPoint,
      bottomRight: PixelPoint
  ): Try[ImageStream] = {
    val (width, height) = getWidthHeight(topLeft, bottomRight, sourceImage)

    Try(Scalr.crop(sourceImage, topLeft.x, topLeft.y, width, height))
      .map(cropped => toImageStream(cropped, image))
  }

  def crop(originalImage: ImageStream, start: PercentPoint, end: PercentPoint): Try[ImageStream] = {
    val sourceImage            = originalImage.sourceImage
    val (topLeft, bottomRight) = transformCoordinates(sourceImage, start, end)
    crop(originalImage, sourceImage, topLeft, bottomRight)
  }

  def crop(originalImage: ImageStream, start: PixelPoint, end: PixelPoint): Try[ImageStream] = {
    val sourceImage = originalImage.sourceImage
    crop(originalImage, sourceImage, start, end)
  }

  private def getStartEndCoords(focalPoint: Int, targetDimensionSize: Int, originalDimensionSize: Int): (Int, Int) = {
    val ts                 = min(targetDimensionSize.toDouble, originalDimensionSize.toDouble) / 2.0
    val (start, end)       = (focalPoint - ts.floor.toInt, focalPoint + ts.round.toInt)
    val (startRem, endRem) = (abs(min(start, 0)), max(end - originalDimensionSize, 0))

    (max(start - endRem, 0), min(end + startRem, originalDimensionSize))
  }

  def dynamicCrop(
      image: ImageStream,
      percentFocalPoint: PercentPoint,
      targetWidthOpt: Option[Int],
      targetHeightOpt: Option[Int],
      ratioOpt: Option[Double]
  ): Try[ImageStream] = {
    val sourceImage               = image.sourceImage
    val focalPoint                = toPixelPoint(percentFocalPoint, sourceImage)
    val (imageWidth, imageHeight) = (sourceImage.getWidth, sourceImage.getHeight)

    val (targetWidth: Int, targetHeight: Int) = (targetWidthOpt, targetHeightOpt, ratioOpt) match {
      case (_, _, Some(ratio))   => minimalCropSizesToPreserveRatio(imageWidth, imageHeight, ratio)
      case (None, None, _)       => return Success(image)
      case (Some(w), Some(h), _) => (min(w, imageWidth), min(h, imageHeight))
      case (Some(w), None, _)    =>
        val actualTargetWidth             = min(imageWidth, w)
        val widthReductionPercent: Double = actualTargetWidth.toDouble / imageWidth.toDouble
        (w, (imageHeight * widthReductionPercent).toInt)
      case (None, Some(h), _) =>
        val actualTargetHeight             = min(imageHeight, h)
        val heightReductionPercent: Double = actualTargetHeight.toDouble / imageHeight.toDouble
        ((imageWidth * heightReductionPercent).toInt, actualTargetHeight)
    }

    val (startY, endY) = getStartEndCoords(focalPoint.y, targetHeight, imageHeight)
    val (startX, endX) = getStartEndCoords(focalPoint.x, targetWidth, imageWidth)

    crop(image, sourceImage, PixelPoint(startX, startY), PixelPoint(endX, endY))
  }

  def minimalCropSizesToPreserveRatio(imageWidth: Int, imageHeight: Int, ratio: Double): (Int, Int) = {
    val newHeight = Math.min(imageWidth / ratio, imageHeight.toDouble).toInt
    val newWidth  = Math.min(newHeight * ratio, imageWidth.toDouble).toInt
    (newWidth, newHeight)
  }

  // Given two sets of coordinates; reorganize them so that the first coordinate is the top-left,
  // and the other coordinate is the bottom-right
  private[service] def transformCoordinates(
      image: BufferedImage,
      start: PercentPoint,
      end: PercentPoint
  ): (PixelPoint, PixelPoint) = {
    val topLeft     = PercentPoint(min(start.x, end.x), min(start.y, end.y))
    val bottomRight = PercentPoint(max(start.x, end.x), max(start.y, end.y))

    (toPixelPoint(topLeft, image), toPixelPoint(bottomRight, image))
  }

  private def toPixelPoint(point: PercentPoint, image: BufferedImage) = {
    val (width, height) = (image.getWidth, image.getHeight)
    PixelPoint((point.normalizedX * width).toInt, (point.normalizedY * height).toInt)
  }

  private[service] def getWidthHeight(start: PixelPoint, end: PixelPoint, image: BufferedImage): (Int, Int) = {
    val width  = abs(start.x - end.x)
    val height = abs(start.y - end.y)
    (min(width, image.getWidth - start.x), min(height, image.getHeight - start.y))
  }

}

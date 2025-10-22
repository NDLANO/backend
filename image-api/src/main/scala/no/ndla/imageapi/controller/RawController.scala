/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import cats.implicits.catsSyntaxEitherId
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.service.{ImageConverter, ImageStorageService, PercentPoint, PixelPoint, ReadService}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{AllErrors, ErrorHandling, DynamicHeaders, ErrorHelpers, TapirController}
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import java.io.InputStream
import scala.util.{Failure, Success, Try}

class RawController(using
    imageStorage: ImageStorageService,
    imageConverter: ImageConverter,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
    readService: ReadService,
    myNDLAApiClient: MyNDLAApiClient,
) extends TapirController {
  import errorHelpers.*
  import errorHandling.*
  override val serviceName: String         = "raw"
  override val prefix: EndpointInput[Unit] = "image-api" / serviceName
  override val enableSwagger: Boolean      = true

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(getImageFileById, getImageFile)

  private def toImageResponse(image: ImageStream): Either[AllErrors, (DynamicHeaders, InputStream)] = {
    val headers = DynamicHeaders.fromValue("Content-Type", image.contentType)
    Right(headers -> image.stream)
  }

  def getImageFile: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch an image with options to resize and crop")
    .description("Fetches a image with options to resize and crop")
    .in(path[String]("image_name").description("The name of the image"))
    .in(EndpointInput.derived[ImageParams])
    .errorOut(errorOutputsFor(404))
    .out(EndpointOutput.derived[DynamicHeaders])
    .out(inputStreamBody)
    .serverLogicPure { case (filePath, imageParams) =>
      getRawImage(filePath, imageParams) match {
        case Failure(ex)  => returnLeftError(ex)
        case Success(img) => toImageResponse(img)
      }
    }

  def getImageFileById: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch an image with options to resize and crop")
    .description("Fetches a image with options to resize and crop")
    .in("id" / path[Long]("image_id").description("The ID of the image"))
    .in(EndpointInput.derived[ImageParams])
    .errorOut(errorOutputsFor(404))
    .out(EndpointOutput.derived[DynamicHeaders])
    .out(inputStreamBody)
    .serverLogicPure { case (imageId, imageParams) =>
      readService.getImageFileName(imageId, imageParams.language) match {
        case Success(Some(fileName)) =>
          val x = getRawImage(fileName, imageParams)
          x match {
            case Failure(ex)  => returnLeftError(ex)
            case Success(img) => toImageResponse(img)
          }
        case Success(None) => notFoundWithMsg(s"Image with id $imageId not found").asLeft
        case Failure(ex)   => returnLeftError(ex)
      }
    }

  private def getRawImage(imageName: String, imageParams: ImageParams): Try[ImageStream] = {
    val dynamicCropOrResize = {
      val canDynamicCrop = canDoDynamicCrop(imageParams)
      if (canDynamicCrop) dynamicCrop
      else {
        resize
      }
    }
    val nonResizableMimeTypes = List("image/gif", "image/svg", "image/svg+xml")
    imageStorage.get(imageName) match {
      case Success(img) if nonResizableMimeTypes.contains(img.contentType.toLowerCase) => Success(img)
      case Success(img)                                                                => crop(img, imageParams)
          .flatMap(stream => dynamicCropOrResize(stream, imageParams))
          .recoverWith {
            case ex: ValidationException => Failure(ex)
            case ex                      =>
              logger.error(s"Could not crop or resize image '$imageName', got exception: '${ex.getMessage}'", ex)
              Success(img)
          }
      case Failure(e) => Failure(e)
    }
  }

  private def doubleInRange(paramName: String, double: Option[Double], from: Int, to: Int): Option[Double] = {
    double match {
      case Some(d) if d >= Math.min(from, to) && d <= Math.max(from, to) => Some(d)
      case Some(d)                                                       => throw ValidationException(errors =
          Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Must be in range $from-$to but was $d"))
        )
      case None => None
    }
  }

  private def crop(image: ImageStream, imageParams: ImageParams): Try[ImageStream] = {
    val unit = imageParams.cropUnit.getOrElse("percent")
    unit match {
      case "percent" =>
        val startX = doubleInRange("cropStartX", imageParams.cropStartX, PercentPoint.MinValue, PercentPoint.MaxValue)
        val startY = doubleInRange("cropStartY", imageParams.cropStartY, PercentPoint.MinValue, PercentPoint.MaxValue)
        val endX   = doubleInRange("cropEndX", imageParams.cropEndX, PercentPoint.MinValue, PercentPoint.MaxValue)
        val endY   = doubleInRange("cropEndY", imageParams.cropEndY, PercentPoint.MinValue, PercentPoint.MaxValue)
        (startX, startY, endX, endY) match {
          case (Some(sx), Some(sy), Some(ex), Some(ey)) =>
            imageConverter.crop(image, PercentPoint(sx, sy), PercentPoint(ex, ey))
          case _ => Success(image)
        }
      case "pixel" =>
        val startX = imageParams.cropStartX.map(_.toInt)
        val startY = imageParams.cropStartY.map(_.toInt)
        val endX   = imageParams.cropEndX.map(_.toInt)
        val endY   = imageParams.cropEndY.map(_.toInt)
        (startX, startY, endX, endY) match {
          case (Some(sx), Some(sy), Some(ex), Some(ey)) =>
            imageConverter.crop(image, PixelPoint(sx, sy), PixelPoint(ex, ey))
          case _ => Success(image)
        }
    }
  }

  private def canDoDynamicCrop(imageParams: ImageParams) = {
    imageParams.focalX.isDefined && imageParams.focalY.isDefined && (imageParams.width.isDefined || imageParams
      .height
      .isDefined || imageParams.ratio.isDefined)
  }

  private def dynamicCrop(image: ImageStream, imageParams: ImageParams): Try[ImageStream] = {

    (imageParams.focalX, imageParams.focalY, imageParams.width, imageParams.height) match {
      case (Some(fx), Some(fy), w, h) =>
        imageConverter.dynamicCrop(image, PercentPoint(fx, fy), w.map(_.toInt), h.map(_.toInt), imageParams.ratio)
      case _ => Success(image)
    }
  }

  private def resize(image: ImageStream, imageParams: ImageParams): Try[ImageStream] = {
    (imageParams.width, imageParams.height) match {
      case (Some(width), Some(height)) => imageConverter.resize(image, width.toInt, height.toInt)
      case (Some(width), _)            => imageConverter.resizeWidth(image, width.toInt)
      case (_, Some(height))           => imageConverter.resizeHeight(image, height.toInt)
      case _                           => Success(image)
    }
  }
}

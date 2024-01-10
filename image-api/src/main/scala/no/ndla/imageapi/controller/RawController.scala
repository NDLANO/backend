/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.controller

import no.ndla.common.errors.ValidationException
import no.ndla.common.model.NDLADate
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.api.{Error, ErrorHelpers}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.{ImageConverter, ImageStorageService, ReadService}
import no.ndla.network.scalatra.NdlaSwaggerSupport
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.servlet.FileUploadSupport
import org.scalatra.swagger.{Parameter, ResponseMessage, Swagger}
import org.scalatra.{NotFound, Ok}

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

trait RawController {
  this: ImageStorageService
    with ImageConverter
    with ImageRepository
    with ErrorHelpers
    with NdlaController
    with Props
    with ReadService
    with NdlaSwaggerSupport =>
  val rawController: RawController

  class RawController(implicit val swagger: Swagger)
      extends NdlaController
      with NdlaSwaggerSupport
      with FileUploadSupport {
    import props.ValidMimeTypes

    protected implicit override val jsonFormats: Formats =
      DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer
    protected val applicationDescription = "API for accessing image files from ndla.no."

    registerModel[Error]()

    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val getImageParams: List[Parameter] = List(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description(
        "Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."
      ),
      queryParam[Option[Int]]("width")
        .description("The target width to resize the image (the unit is pixles). Image proportions are kept intact"),
      queryParam[Option[Int]]("height")
        .description("The target height to resize the image (the unit is pixles). Image proportions are kept intact"),
      queryParam[Option[Int]]("cropStartX").description(
        "The first image coordinate X, in percent (0 to 100), specifying the crop start position. If used the other crop parameters must also be supplied"
      ),
      queryParam[Option[Int]]("cropStartY").description(
        "The first image coordinate Y, in percent (0 to 100), specifying the crop start position. If used the other crop parameters must also be supplied"
      ),
      queryParam[Option[Int]]("cropEndX").description(
        "The end image coordinate X, in percent (0 to 100), specifying the crop end position. If used the other crop parameters must also be supplied"
      ),
      queryParam[Option[Int]]("cropEndY").description(
        "The end image coordinate Y, in percent (0 to 100), specifying the crop end position. If used the other crop parameters must also be supplied"
      ),
      queryParam[Option[Int]]("focalX").description(
        "The end image coordinate X, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied"
      ),
      queryParam[Option[Int]]("focalY").description(
        "The end image coordinate Y, in percent (0 to 100), specifying the focal point. If used the other focal point parameter, width and/or height, must also be supplied"
      ),
      queryParam[Option[Double]]("ratio").description(
        "The wanted aspect ratio, defined as width/height. To be used together with the focal parameters. If used the width and height is ignored and derived from the aspect ratio instead."
      )
    )

    get(
      "/:image_name",
      operation(
        apiOperation[ImageStream]("getImageFile")
          .summary("Fetch an image with options to resize and crop")
          .description("Fetches a image with options to resize and crop")
          .produces(ValidMimeTypes :+ "application/octet-stream": _*)
          .parameters(
            List[Parameter](pathParam[String]("image_name").description("The name of the image"))
              ++ getImageParams: _*
          )
          .responseMessages(response404, response500)
      )
    ) {
      val filePath = params("image_name")
      getRawImage(filePath) match {
        case Failure(ex)  => errorHandler(ex)
        case Success(img) => Ok(img)
      }
    }: Unit

    get(
      "/id/:image_id",
      operation(
        apiOperation[ImageStream]("getImageFileById")
          .summary("Fetch an image with options to resize and crop")
          .description("Fetches a image with options to resize and crop")
          .produces(ValidMimeTypes :+ "application/octet-stream": _*)
          .parameters(
            List[Parameter](
              pathParam[String]("image_id").description("The ID of the image"),
              queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language.")
            )
              ++ getImageParams: _*
          )
          .responseMessages(response404, response500)
      )
    ) {
      val imageId  = long("image_id")
      val language = paramOrNone("language")

      readService.getImageFileName(imageId, language) match {
        case Some(fileName) =>
          getRawImage(fileName) match {
            case Failure(ex)  => errorHandler(ex)
            case Success(img) => Ok(img)
          }
        case None => NotFound(Error(ErrorHelpers.NOT_FOUND, s"Image with id $imageId not found"))
      }
    }: Unit

    private def getRawImage(imageName: String): Try[ImageStream] = {
      val dynamicCropOrResize =
        if (canDoDynamicCrop) dynamicCrop _
        else {
          resize _
        }
      val nonResizableMimeTypes = List("image/gif", "image/svg", "image/svg+xml")
      imageStorage.get(imageName) match {
        case Success(img) if nonResizableMimeTypes.contains(img.contentType.toLowerCase) => Success(img)
        case Success(img) =>
          crop(img)
            .flatMap(dynamicCropOrResize)
            .recoverWith {
              case ex: ValidationException => Failure(ex)
              case ex =>
                logger.error(s"Could not crop or resize image '$imageName', got exception: '${ex.getMessage}'", ex)
                Success(img)
            }
        case Failure(e) => Failure(e)
      }
    }

    private def crop(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      val startX = doubleInRange("cropStartX", PercentPoint.MinValue, PercentPoint.MaxValue)
      val startY = doubleInRange("cropStartY", PercentPoint.MinValue, PercentPoint.MaxValue)
      val endX   = doubleInRange("cropEndX", PercentPoint.MinValue, PercentPoint.MaxValue)
      val endY   = doubleInRange("cropEndY", PercentPoint.MinValue, PercentPoint.MaxValue)

      (startX, startY, endX, endY) match {
        case (Some(sx), Some(sy), Some(ex), Some(ey)) =>
          imageConverter.crop(image, PercentPoint(sx, sy), PercentPoint(ex, ey))
        case _ => Success(image)
      }
    }

    private def canDoDynamicCrop(implicit request: HttpServletRequest) =
      doubleOrNone("focalX").isDefined && doubleOrNone("focalY").isDefined && (doubleOrNone(
        "width"
      ).isDefined || doubleOrNone("height").isDefined || doubleOrNone("ratio").isDefined)

    private def dynamicCrop(image: ImageStream): Try[ImageStream] = {
      val focalX                = doubleInRange("focalX", PercentPoint.MinValue, PercentPoint.MaxValue)
      val focalY                = doubleInRange("focalY", PercentPoint.MinValue, PercentPoint.MaxValue)
      val ratio                 = doubleOrNone("ratio")
      val (widthOpt, heightOpt) = extractDoubleOpt2("width", "height")

      (focalX, focalY, widthOpt, heightOpt) match {
        case (Some(fx), Some(fy), w, h) =>
          imageConverter.dynamicCrop(image, PercentPoint(fx, fy), w.map(_.toInt), h.map(_.toInt), ratio)
        case _ => Success(image)
      }
    }

    private def resize(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      val (widthOpt, heightOpt) = extractDoubleOpt2("width", "height")
      (widthOpt, heightOpt) match {
        case (Some(width), Some(height)) => imageConverter.resize(image, width.toInt, height.toInt)
        case (Some(width), _)            => imageConverter.resizeWidth(image, width.toInt)
        case (_, Some(height))           => imageConverter.resizeHeight(image, height.toInt)
        case _                           => Success(image)
      }
    }

  }
}

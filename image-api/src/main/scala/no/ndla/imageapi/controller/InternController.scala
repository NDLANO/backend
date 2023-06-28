/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.model.api.{Error, ErrorHelpers}
import no.ndla.imageapi.model.domain.{DBImageMetaInformation, ImageMetaInformation}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageIndexService, TagIndexService}
import no.ndla.imageapi.service.{ConverterService, ReadService}
import no.ndla.network.tapir.auth.TokenUser
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.Swagger
import org.scalatra.{BadRequest, InternalServerError, NotFound, Ok}

import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

trait InternController {
  this: ImageRepository
    with ReadService
    with ConverterService
    with ImageIndexService
    with TagIndexService
    with ImageRepository
    with DBImageMetaInformation
    with NdlaController
    with Props
    with ErrorHelpers =>
  val internController: InternController

  class InternController(implicit val swagger: Swagger) extends NdlaController {
    protected val applicationDescription                 = "API for accessing internal functionality in image API"
    protected implicit override val jsonFormats: Formats = DefaultFormats ++ ImageMetaInformation.jsonEncoders

    post("/index") {
      val numShards   = intOrNone("numShards")
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

      val indexResults = for {
        imageIndex <- Future { imageIndexService.indexDocuments(numShards) }
        tagIndex   <- Future { tagIndexService.indexDocuments(numShards) }
      } yield (imageIndex, tagIndex)

      Await.result(indexResults, Duration(60, TimeUnit.MINUTES)) match {
        case (Success(imageIndex), Success(tagIndex)) =>
          val indexTime = math.max(tagIndex.millisUsed, imageIndex.millisUsed)
          val result =
            s"Completed indexing of ${imageIndex.totalIndexed} images in $indexTime ms."
          logger.info(result)
          Ok(result)
        case (Failure(imageFail), _) =>
          logger.warn(imageFail.getMessage, imageFail)
          InternalServerError(imageFail.getMessage)
        case (_, Failure(tagFail)) =>
          logger.warn(tagFail.getMessage, tagFail)
          InternalServerError(tagFail.getMessage)
      }
    }: Unit

    delete("/index") {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      val deleteResults = imageIndexService.findAllIndexes(props.SearchIndex) match {
        case Failure(f) => halt(status = 500, body = f.getMessage)
        case Success(indexes) =>
          indexes.map(index => {
            logger.info(s"Deleting index $index")
            imageIndexService.deleteIndexWithName(Option(index))
          })
      }
      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
      }
    }: Unit

    get("/extern/:image_id") {
      val externalId = params("image_id")
      val language   = paramOrNone("language")
      val user       = TokenUser.fromScalatraRequest(request).toOption

      imageRepository.withExternalId(externalId) match {
        case Some(image) =>
          converterService.asApiImageMetaInformationWithDomainUrlV2(image, language, user) match {
            case Failure(ex)        => errorHandler(ex)
            case Success(converted) => Ok(converted)
          }
        case None => NotFound(Error(ErrorHelpers.NOT_FOUND, s"Image with external id $externalId not found"))
      }
    }: Unit

    get("/domain_image_from_url/") {
      val urlQueryParam = "url"
      val url           = paramOrNone(urlQueryParam)
      url match {
        case Some(p) =>
          readService.getDomainImageMetaFromUrl(p) match {
            case Success(image) => Ok(image)
            case Failure(ex)    => errorHandler(ex)
          }
        case None =>
          BadRequest(
            Error(ErrorHelpers.VALIDATION, s"Query param '$urlQueryParam' needs to be specified to return an image")
          )
      }
    }: Unit

    get("/dump/image/") {
      val pageNo   = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)
      readService.getMetaImageDomainDump(pageNo, pageSize)
    }: Unit

    get("/dump/image/:id") {
      val id = long("id")
      imageRepository.withId(id) match {
        case Some(image) => Ok(image)
        case None        => errorHandler(new ImageNotFoundException(s"Could not find image with id: '$id'"))
      }
    }: Unit

    post("/dump/image/") {
      val domainMeta = extract[ImageMetaInformation](request.body)
      Ok(imageRepository.insert(domainMeta))
    }: Unit
  }

}

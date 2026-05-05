/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.errors.{NotFoundException, ValidationException}
import no.ndla.common.implicits.toTry
import no.ndla.imageapi.model.api.bulk.{BulkUploadStateDTO, BulkUploadStatus}
import no.ndla.imageapi.model.api.{ImageMetaDomainDumpDTO, ImageMetaInformationV2DTO, ImageMetaInformationV3DTO}
import no.ndla.imageapi.model.domain.{ImageFileData, ImageMetaInformation, Sort}
import no.ndla.imageapi.model.{ImageConversionException, ImageNotFoundException, InvalidUrlException, api}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{SearchConverterService, TagSearchService}
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.network.tapir.auth.TokenUser
import ox.flow.Flow

import java.util.UUID
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.{Failure, Success, Try}

class ReadService(using
    converterService: ConverterService,
    imageRepository: ImageRepository,
    tagSearchService: TagSearchService,
    searchConverterService: SearchConverterService,
    bulkUploadStore: BulkUploadStore,
) extends StrictLogging {

  private val bulkUploadPollInterval: FiniteDuration = 500.millis

  def withIdV3(
      imageId: Long,
      language: Option[String],
      user: Option[TokenUser],
  ): Try[Option[ImageMetaInformationV3DTO]] = {
    imageRepository
      .withId(imageId)
      .flatMap(maybeImage =>
        maybeImage.traverse(image => converterService.asApiImageMetaInformationV3(image, language, user))
      )
  }

  def getAllTags(
      input: String,
      pageSize: Int,
      page: Int,
      language: String,
      sort: Sort,
  ): Try[api.TagsSearchResultDTO] = {
    val result = tagSearchService.matchingQuery(
      query = input,
      searchLanguage = language,
      page = page,
      pageSize = pageSize,
      sort = sort,
    )

    result.map(searchConverterService.tagSearchResultAsApiResult)
  }

  def withId(imageId: Long, language: Option[String], user: Option[TokenUser]): Try[Option[ImageMetaInformationV2DTO]] =
    imageRepository
      .withId(imageId)
      .flatMap(maybeImage =>
        maybeImage.traverse(image =>
          converterService.asApiImageMetaInformationWithApplicationUrlV2(image, language, user)
        )
      )

  def getImagesByIdsV3(
      ids: List[Long],
      language: Option[String],
      user: Option[TokenUser],
  ): Try[List[ImageMetaInformationV3DTO]] = {
    if (ids.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
    else imageRepository
      .withIds(ids)
      .flatMap(images => images.traverse(image => converterService.asApiImageMetaInformationV3(image, language, user)))
  }

  private def handleIdPathParts(pathParts: List[String]): Try[ImageMetaInformation] = Try(pathParts(3).toLong) match {
    case Failure(_)  => Failure(InvalidUrlException("Could not extract id from id url."))
    case Success(id) => imageRepository.withId(id) match {
        case Success(Some(image)) => Success(image)
        case Success(None)        =>
          Failure(new ImageNotFoundException(s"Extracted id '$id', but no image with that id was found"))
        case Failure(ex) => Failure(ex)
      }
  }

  private def handleRawPathParts(pathParts: List[String]): Try[ImageMetaInformation] = pathParts.lift(2) match {
    case Some(path) if path.nonEmpty => getImageMetaFromFilePath(path)
    case _                           => Failure(InvalidUrlException("Could not extract path from url."))
  }

  def getImageFileFromFilePath(path: String): Try[ImageFileData] = getImageMetaFromFilePath(path).flatMap { image =>
    image
      .images
      .find(i => i.fileName.dropWhile(_ == '/') == path.dropWhile(_ == '/'))
      .toTry(
        ImageConversionException(s"Image path '$path' was found in database, but not found in metadata. This is a bug.")
      )
  }

  private def getImageMetaFromFilePath(path: String): Try[ImageMetaInformation] = imageRepository
    .getImageFromFilePath(path)
    .flatMap(_.toTry(ImageNotFoundException(s"Extracted path '$path', but no image with that path was found")))

  def getDomainImageMetaFromUrl(url: String): Try[ImageMetaInformation] = {
    val pathParts          = url.path.parts.toList
    val isRawControllerUrl = pathParts.slice(0, 2) == List("image-api", "raw")
    val isIdUrl            = pathParts.slice(0, 3) == List("image-api", "raw", "id")

    if (isIdUrl) handleIdPathParts(pathParts)
    else if (isRawControllerUrl) handleRawPathParts(pathParts)
    else Failure(InvalidUrlException("Could not extract id or path from url."))
  }

  def getMetaImageDomainDump(pageNo: Int, pageSize: Int): Try[ImageMetaDomainDumpDTO] = {
    val (safePageNo, safePageSize) = (math.max(pageNo, 1), math.max(pageSize, 0))
    for {
      results <- imageRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)
      count   <- imageRepository.imageCount
      dump     = ImageMetaDomainDumpDTO(count, pageNo, pageSize, results)
    } yield dump
  }

  def getImageFileName(imageId: Long, language: Option[String]): Try[Option[String]] = {
    imageRepository
      .withId(imageId)
      .map {
        _.flatMap { imageMeta =>
          findByLanguageOrBestEffort(imageMeta.images, language).map(_.fileName.dropWhile(_ == '/'))
        }
      }
  }

  /** Streams the state of a bulk upload session. Polls Redis on a fixed interval and emits a new state whenever it
    * changes. Once a terminal status (`Complete` or `Failed`) is observed it is emitted and the flow is closed.
    *
    * If no state is found for the given uploadId a [[NotFoundException]] is returned so the controller can map it to a
    * 404.
    */
  def getStatusStreamOfBulkUpload(uploadId: UUID): Try[Flow[BulkUploadStateDTO]] = {
    bulkUploadStore
      .get(uploadId)
      .flatMap {
        case None          => Failure(NotFoundException(s"No bulk upload with id $uploadId"))
        case Some(initial) =>
          val pollFlow: Flow[BulkUploadStateDTO] = Flow
            .tick(bulkUploadPollInterval)
            .mapStateful(initial)((prevState, _) =>
              bulkUploadStore.get(uploadId) match {
                case Success(Some(state)) if state == prevState => prevState -> None
                case Success(Some(state))                       => state     -> Some(state)
                case Success(None)                              =>
                  val failed = prevState.asFailed(s"Bulk upload $uploadId no longer exists")
                  failed -> Some(failed)
                case Failure(ex) =>
                  logger.error(s"Bulk upload $uploadId: failed to read state from redis", ex)
                  prevState -> None
              }
            )
            .collect { case Some(state) =>
              state
            }
            .takeWhile(state => !isTerminal(state), includeFirstFailing = true)

          val combined =
            if (isTerminal(initial)) Flow.fromValues(initial)
            else Flow.fromValues(initial).concat(pollFlow)

          Success(combined)
      }
  }

  private def isTerminal(state: BulkUploadStateDTO): Boolean = state.status match {
    case BulkUploadStatus.Complete | BulkUploadStatus.Failed => true
    case BulkUploadStatus.Pending | BulkUploadStatus.Running => false
  }
}

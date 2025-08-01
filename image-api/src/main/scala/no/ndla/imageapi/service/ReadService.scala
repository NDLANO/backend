/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.UrlPath
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.imageapi.model.api.{ImageMetaDomainDumpDTO, ImageMetaInformationV2DTO, ImageMetaInformationV3DTO}
import no.ndla.imageapi.model.domain.{ImageFileData, ImageMetaInformation, Sort}
import no.ndla.imageapi.model.{ImageConversionException, ImageNotFoundException, InvalidUrlException, api}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageIndexService, SearchConverterService, TagSearchService}
import no.ndla.language.Language.findByLanguageOrBestEffort
import cats.implicits.*
import no.ndla.common.errors.ValidationException
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: ConverterService & ValidationService & ImageRepository & ImageIndexService & ImageStorageService &
    TagSearchService & SearchConverterService =>
  val readService: ReadService

  class ReadService extends StrictLogging {

    def withIdV3(
        imageId: Long,
        language: Option[String],
        user: Option[TokenUser]
    ): Try[Option[ImageMetaInformationV3DTO]] = {
      imageRepository
        .withId(imageId)
        .traverse(image => converterService.asApiImageMetaInformationV3(image, language, user))
    }

    def getAllTags(
        input: String,
        pageSize: Int,
        page: Int,
        language: String,
        sort: Sort
    ): Try[api.TagsSearchResultDTO] = {
      val result = tagSearchService.matchingQuery(
        query = input,
        searchLanguage = language,
        page = page,
        pageSize = pageSize,
        sort = sort
      )

      result.map(searchConverterService.tagSearchResultAsApiResult)
    }

    def withId(
        imageId: Long,
        language: Option[String],
        user: Option[TokenUser]
    ): Try[Option[ImageMetaInformationV2DTO]] =
      imageRepository
        .withId(imageId)
        .traverse(image => converterService.asApiImageMetaInformationWithApplicationUrlV2(image, language, user))

    def getImagesByIdsV3(
        ids: List[Long],
        language: Option[String],
        user: Option[TokenUser]
    ): Try[List[ImageMetaInformationV3DTO]] = {
      if (ids.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
      else
        imageRepository
          .withIds(ids)
          .traverse(image => converterService.asApiImageMetaInformationV3(image, language, user))
    }

    private def handleIdPathParts(pathParts: List[String]): Try[ImageMetaInformation] =
      Try(pathParts(3).toLong) match {
        case Failure(_)  => Failure(InvalidUrlException("Could not extract id from id url."))
        case Success(id) =>
          imageRepository.withId(id) match {
            case Some(image) => Success(image)
            case None => Failure(new ImageNotFoundException(s"Extracted id '$id', but no image with that id was found"))
          }
      }

    private def urlEncodePath(path: String) = UrlPath.parse(path).toString

    private def handleRawPathParts(pathParts: List[String]): Try[ImageMetaInformation] =
      pathParts.lift(2) match {
        case Some(path) if path.nonEmpty => getImageMetaFromFilePath(path)
        case _                           => Failure(InvalidUrlException("Could not extract path from url."))
      }

    def getImageFromFilePath(path: String): Try[ImageFileData] = {
      val encodedPath = urlEncodePath(path)
      imageRepository.getImageFromFilePath(encodedPath) match {
        case Some(image) =>
          image.images
            .getOrElse(Seq.empty)
            .find(i => i.fileName.dropWhile(_ == '/') == path.dropWhile(_ == '/')) match {
            case Some(img) => Success(img)
            case None      =>
              Failure(
                ImageConversionException(
                  s"Image path '$path' was found in database, but not found in metadata. This is a bug."
                )
              )
          }
        case None =>
          Failure(new ImageNotFoundException(s"Extracted path '$encodedPath', but no image with that path was found"))
      }

    }

    def getImageMetaFromFilePath(path: String): Try[ImageMetaInformation] = {
      val encodedPath = urlEncodePath(path)
      imageRepository.getImageFromFilePath(encodedPath) match {
        case Some(image) => Success(image)
        case None        =>
          Failure(new ImageNotFoundException(s"Extracted path '$encodedPath', but no image with that path was found"))
      }
    }

    def getDomainImageMetaFromUrl(url: String): Try[ImageMetaInformation] = {
      val pathParts          = url.path.parts.toList
      val isRawControllerUrl = pathParts.slice(0, 2) == List("image-api", "raw")
      val isIdUrl            = pathParts.slice(0, 3) == List("image-api", "raw", "id")

      if (isIdUrl) handleIdPathParts(pathParts)
      else if (isRawControllerUrl) handleRawPathParts(pathParts)
      else Failure(InvalidUrlException("Could not extract id or path from url."))
    }

    def getMetaImageDomainDump(pageNo: Int, pageSize: Int): ImageMetaDomainDumpDTO = {
      val (safePageNo, safePageSize) = (math.max(pageNo, 1), math.max(pageSize, 0))
      val results                    = imageRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)

      ImageMetaDomainDumpDTO(imageRepository.imageCount, pageNo, pageSize, results)
    }

    def getImageFileName(imageId: Long, language: Option[String]): Try[Option[String]] = {
      val imageMeta = for {
        imageMeta     <- imageRepository.withId(imageId)
        imageFileMeta <- findByLanguageOrBestEffort(imageMeta.images.getOrElse(Seq.empty), language)
      } yield imageFileMeta

      imageMeta.traverse(meta =>
        UrlPath
          .parseTry(meta.fileName)
          .map(_.toStringRaw.dropWhile(_ == '/'))
      )
    }
  }

}

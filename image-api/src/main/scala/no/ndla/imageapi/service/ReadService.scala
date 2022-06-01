/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.{Uri, UrlPath}
import io.lemonlabs.uri.typesafe.dsl._
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model.api.ImageMetaInformationV2
import no.ndla.imageapi.model.domain.{Image, ImageMetaInformation, Sort}
import no.ndla.imageapi.model.{ImageConversionException, ImageNotFoundException, InvalidUrlException, api}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageIndexService, SearchConverterService, TagSearchService}
import no.ndla.language.Language.findByLanguageOrBestEffort
import cats.implicits._

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: ConverterService
    with ValidationService
    with ImageRepository
    with ImageIndexService
    with ImageStorageService
    with TagSearchService
    with SearchConverterService
    with Clock
    with User =>
  val readService: ReadService

  class ReadService extends LazyLogging {

    def getAllTags(input: String, pageSize: Int, page: Int, language: String, sort: Sort): Try[api.TagsSearchResult] = {
      val result = tagSearchService.matchingQuery(
        query = input,
        searchLanguage = language,
        page = page,
        pageSize = pageSize,
        sort = sort
      )

      result.map(searchConverterService.tagSearchResultAsApiResult)
    }

    def withId(imageId: Long, language: Option[String]): Try[Option[ImageMetaInformationV2]] =
      imageRepository
        .withId(imageId)
        .traverse(image => converterService.asApiImageMetaInformationWithApplicationUrlV2(image, language))

    private def handleIdPathParts(pathParts: List[String]): Try[ImageMetaInformation] =
      Try(pathParts(3).toLong) match {
        case Failure(_) => Failure(new InvalidUrlException("Could not extract id from id url."))
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
        case _                           => Failure(new InvalidUrlException("Could not extract path from url."))
      }

    def getImageFromFilePath(path: String): Try[Image] = {
      val encodedPath = urlEncodePath(path)
      imageRepository.getImageFromFilePath(encodedPath) match {
        case Some(image) =>
          image.images.find(i => i.fileName.dropWhile(_ == '/') == path.dropWhile(_ == '/')) match {
            case Some(img) => Success(img)
            case None =>
              Failure(
                ImageConversionException(
                  "Image path was found in database, but not found in metadata. This is a bug."
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
        case None =>
          Failure(new ImageNotFoundException(s"Extracted path '$encodedPath', but no image with that path was found"))
      }
    }

    def getDomainImageMetaFromUrl(url: String): Try[ImageMetaInformation] = {
      val pathParts          = url.path.parts.toList
      val isRawControllerUrl = pathParts.slice(0, 2) == List("image-api", "raw")
      val isIdUrl            = pathParts.slice(0, 3) == List("image-api", "raw", "id")

      if (isIdUrl) handleIdPathParts(pathParts)
      else if (isRawControllerUrl) handleRawPathParts(pathParts)
      else Failure(new InvalidUrlException("Could not extract id or path from url."))
    }

    def getMetaImageDomainDump(pageNo: Int, pageSize: Int): api.ImageMetaDomainDump = {
      val (safePageNo, safePageSize) = (math.max(pageNo, 1), math.max(pageSize, 0))
      val results                    = imageRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)

      api.ImageMetaDomainDump(imageRepository.imageCount, pageNo, pageSize, results)
    }

    def getImageFileName(imageId: Long, language: Option[String]): Option[String] = {
      for {
        imageMeta     <- imageRepository.withId(imageId)
        imageFileMeta <- findByLanguageOrBestEffort(imageMeta.images, language)
        imageName = Uri.parse(imageFileMeta.fileName).toStringRaw.dropWhile(_ == '/')
      } yield imageName
    }
  }

}

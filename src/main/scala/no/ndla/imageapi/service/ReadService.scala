package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.dsl._
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model.api.{ImageId, ImageMetaInformationV2}
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexService
import no.ndla.imageapi.model.{ImageNotFoundException, InvalidUrlException, ValidationException}

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: ConverterService
    with ValidationService
    with ImageRepository
    with IndexService
    with ImageStorageService
    with Clock
    with User =>
  val readService: ReadService

  class ReadService extends LazyLogging {

    def withId(imageId: Long, language: Option[String]): Option[ImageMetaInformationV2] =
      imageRepository
        .withId(imageId)
        .flatMap(image => converterService.asApiImageMetaInformationWithApplicationUrlV2(image, language))

    def getIdFromIdPath(path: String): Option[Long] = { ??? }

    def getFilePathFromRawPath(path: String): Option[String] = { ??? }

    def getImageIdFromUrl(url: String): Try[ImageId] = {
      getDomainImageMetaFromUrl(url).flatMap(image => Try(ImageId(image.id.get)))
    }

    private def handleIdPathParts(pathParts: List[String]): Try[ImageMetaInformation] =
      Try(pathParts(3).toLong) match {
        case Failure(_) => Failure(new InvalidUrlException("Could not extract id from id url."))
        case Success(id) =>
          imageRepository.withId(id) match {
            case Some(image) => Success(image)
            case None        => Failure(new ImageNotFoundException(s"Extracted id '$id', but no image with that id was found"))
          }
      }

    private def handleRawPathPathParts(pathParts: List[String]): Try[ImageMetaInformation] =
      pathParts.lift(2) match {
        case Some(path) if path.size > 0 =>
          imageRepository.getImageFromFilePath(s"/$path") match {
            case Some(image) => Success(image)
            case None =>
              Failure(new ImageNotFoundException(s"Extracted path '$path', but no image with that path was found"))
          }
        case _ => Failure(new InvalidUrlException("Could not extract path from url."))
      }

    private[service] def getDomainImageMetaFromUrl(url: String): Try[ImageMetaInformation] = {
      val pathParts = url.path.parts.toList
      val isRawControllerUrl = pathParts.slice(0, 2) == List("image-api", "raw")
      val isIdUrl = pathParts.slice(0, 3) == List("image-api", "raw", "id")

      if (isIdUrl) handleIdPathParts(pathParts)
      else if (isRawControllerUrl) handleRawPathPathParts(pathParts)
      else Failure(new InvalidUrlException("Could not extract id or path from url."))
    }
  }

}

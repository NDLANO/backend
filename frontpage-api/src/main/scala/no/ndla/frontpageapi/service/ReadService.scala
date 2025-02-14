/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import cats.implicits.*
import no.ndla.common.errors as common
import no.ndla.common.implicits.*
import no.ndla.common.model.api.FrontPageDTO
import no.ndla.frontpageapi.model.api
import no.ndla.frontpageapi.model.api.{SubjectPageDomainDumpDTO, SubjectPageIdDTO}
import no.ndla.frontpageapi.model.domain.Errors.{LanguageNotFoundException, SubjectPageNotFoundException}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: SubjectPageRepository & FrontPageRepository & FilmFrontPageRepository & ConverterService =>
  val readService: ReadService

  class ReadService {

    private def validateSubjectPageIdsOrError(subjectIds: List[Long]): Try[List[Long]] = {
      if (subjectIds.isEmpty) Failure(common.ValidationException("ids", "Query parameter 'ids' is missing"))
      else Success(subjectIds)
    }

    def getIdFromExternalId(nid: String): Try[Option[SubjectPageIdDTO]] =
      subjectPageRepository.getIdFromExternalId(nid) match {
        case Success(Some(id)) => Success(Some(SubjectPageIdDTO(id)))
        case Success(None)     => Success(None)
        case Failure(ex)       => Failure(ex)
      }

    def subjectPage(id: Long, language: String, fallback: Boolean): Try[api.SubjectPageDTO] = {
      val maybeSubject = subjectPageRepository.withId(id).?
      val converted    = maybeSubject.traverse(ConverterService.toApiSubjectPage(_, language, fallback)).?
      converted.toTry(SubjectPageNotFoundException(id))
    }

    def subjectPages(
        page: Int,
        pageSize: Int,
        language: String,
        fallback: Boolean
    ): Try[List[api.SubjectPageDTO]] = {
      val offset    = pageSize * (page - 1)
      val data      = subjectPageRepository.all(offset, pageSize).?
      val converted = data.map(ConverterService.toApiSubjectPage(_, language, fallback))
      val filtered  = filterOutNotFoundExceptions(converted)
      filtered.sequence
    }

    private def filterOutNotFoundExceptions[T](exceptions: List[Try[T]]): List[Try[T]] = {
      exceptions.filter {
        case Failure(_: SubjectPageNotFoundException) => false
        case Failure(_: LanguageNotFoundException)    => false
        case _                                        => true
      }
    }

    def getSubjectPageByIds(
        subjectIds: List[Long],
        language: String,
        fallback: Boolean,
        pageSize: Int,
        page: Int
    ): Try[List[api.SubjectPageDTO]] = {
      val offset = (page - 1) * pageSize
      for {
        ids          <- validateSubjectPageIdsOrError(subjectIds)
        subjectPages <- subjectPageRepository.withIds(ids, offset, pageSize)
        api          <- subjectPages.traverse(subject => ConverterService.toApiSubjectPage(subject, language, fallback))
      } yield api
    }

    def getFrontPage: Try[FrontPageDTO] = {
      frontPageRepository.getFrontPage.flatMap {
        case None        => Failure(common.NotFoundException("Front page was not found"))
        case Some(value) => Success(ConverterService.toApiFrontPage(value))
      }
    }

    def filmFrontPage(language: Option[String]): Option[api.FilmFrontPageDTO] = {
      filmFrontPageRepository.get.map(page => ConverterService.toApiFilmFrontPage(page, language))
    }
  }
}

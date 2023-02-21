/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.service

import cats.implicits._
import no.ndla.common.errors.ValidationException
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.frontpageapi.model.api
import no.ndla.frontpageapi.model.api.SubjectPageId
import no.ndla.frontpageapi.model.domain.Errors.{LanguageNotFoundException, NotFoundException}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: SubjectPageRepository with FrontPageRepository with FilmFrontPageRepository with ConverterService =>
  val readService: ReadService

  class ReadService {

    private def validateSubjectPageIdsOrError(subjectIds: List[Long]): Try[List[Long]] = {
      if (subjectIds.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
      else Success(subjectIds)
    }

    def getIdFromExternalId(nid: String): Try[Option[SubjectPageId]] =
      subjectPageRepository.getIdFromExternalId(nid) match {
        case Success(Some(id)) => Success(Some(SubjectPageId(id)))
        case Success(None)     => Success(None)
        case Failure(ex)       => Failure(ex)
      }

    def subjectPage(id: Long, language: String, fallback: Boolean = false): Option[api.SubjectPageData] =
      subjectPageRepository
        .withId(id)
        .map(sub => ConverterService.toApiSubjectPage(sub, language, fallback))
        .collect { case Success(sub) => sub }

    def subjectPages(page: Int, pageSize: Int, language: String, fallback: Boolean): Try[List[api.SubjectPageData]] = {
      val offset    = pageSize * (page - 1)
      val data      = subjectPageRepository.all(offset, pageSize).?
      val converted = data.map(ConverterService.toApiSubjectPage(_, language, fallback))
      val filtered  = filterOutNotFoundExceptions(converted)
      filtered.sequence
    }

    private def filterOutNotFoundExceptions[T](exceptions: List[Try[T]]): List[Try[T]] = {
      exceptions.filter {
        case Failure(_: NotFoundException)         => false
        case Failure(_: LanguageNotFoundException) => false
        case _                                     => true
      }
    }

    def getSubjectPageByIds(
        subjectIds: List[Long],
        language: String,
        fallback: Boolean,
        pageSize: Long,
        page: Long
    ): Try[List[api.SubjectPageData]] = {
      val offset = (page - 1) * pageSize
      for {
        ids          <- validateSubjectPageIdsOrError(subjectIds)
        subjectPages <- subjectPageRepository.withIds(ids, offset, pageSize)
        api          <- subjectPages.traverse(subject => ConverterService.toApiSubjectPage(subject, language, fallback))
      } yield api
    }

    def frontPage: Option[api.FrontPageData] = {
      frontPageRepository.get.map(ConverterService.toApiFrontPage)
    }

    def filmFrontPage(language: Option[String]): Option[api.FilmFrontPageData] = {
      filmFrontPageRepository.get.map(page => ConverterService.toApiFilmFrontPage(page, language))
    }
  }
}

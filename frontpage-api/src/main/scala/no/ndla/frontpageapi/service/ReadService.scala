/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.service

import cats.implicits._
import no.ndla.common.errors.{NotFoundException, ValidationException}
import no.ndla.frontpageapi.model.api
import no.ndla.frontpageapi.model.api.SubjectPageId
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

    def getIdFromExternalId(nid: String): Try[SubjectPageId] =
      subjectPageRepository.getIdFromExternalId(nid).flatMap {
        case Some(id) => Success(SubjectPageId(id))
        case None     => Failure(NotFoundException(s"Subject page with external id $nid was not found"))
      }

    def subjectPage(id: Long, language: String, fallback: Boolean = false): Try[api.SubjectPageData] =
      subjectPageRepository
        .withId(id)
        .flatMap {
          case None              => Failure(NotFoundException(s"Subject page with id $id was not found"))
          case Some(subjectPage) => Success(subjectPage)
        }
        .flatMap(sub => ConverterService.toApiSubjectPage(sub, language, fallback))

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
        z            <- Failure(ValidationException("yes", "hehe"))
      } yield z
    }

    def frontPage: Option[api.FrontPageData] = {
      frontPageRepository.get.map(ConverterService.toApiFrontPage)
    }

    def filmFrontPage(language: Option[String]): Option[api.FilmFrontPageData] = {
      filmFrontPageRepository.get.map(page => ConverterService.toApiFilmFrontPage(page, language))
    }
  }
}

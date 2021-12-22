/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.service

import org.log4s.getLogger
import no.ndla.frontpageapi.model.api
import no.ndla.frontpageapi.model.api.SubjectPageId
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: SubjectPageRepository with FrontPageRepository with FilmFrontPageRepository =>
  val readService: ReadService

  class ReadService {

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

    def frontPage: Option[api.FrontPageData] = {
      frontPageRepository.get.map(ConverterService.toApiFrontPage)
    }

    def filmFrontPage(language: Option[String]): Option[api.FilmFrontPageData] = {
      filmFrontPageRepository.get.map(page => ConverterService.toApiFilmFrontPage(page, language))
    }
  }
}

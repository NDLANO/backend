/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.service

import no.ndla.frontpageapi.FrontpageApiProperties
import no.ndla.frontpageapi.model.domain.Errors.{NotFoundException, OperationNotAllowedException, ValidationException}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.model.{api, domain}

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: SubjectPageRepository with FrontPageRepository with FilmFrontPageRepository =>
  val writeService: WriteService

  class WriteService {

    def newSubjectPage(subject: api.NewSubjectFrontPageData): Try[api.SubjectPageData] = {
      for {
        convertedSubject <- ConverterService.toDomainSubjectPage(subject)
        subjectPage <- subjectPageRepository.newSubjectPage(convertedSubject, subject.externalId.getOrElse(""))
        converted <- ConverterService.toApiSubjectPage(subjectPage, FrontpageApiProperties.DefaultLanguage, true)
      } yield converted
    }

    def updateSubjectPage(id: Long,
                          subject: api.NewSubjectFrontPageData,
                          language: String): Try[api.SubjectPageData] = {
      subjectPageRepository.exists(id) match {
        case Success(exists) if exists =>
          for {
            domainSubject <- ConverterService.toDomainSubjectPage(id, subject)
            subjectPage <- subjectPageRepository.updateSubjectPage(domainSubject)
            converted <- ConverterService.toApiSubjectPage(subjectPage, language, true)
          } yield converted
        case Success(_) =>
          Failure(NotFoundException(id))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateSubjectPage(id: Long,
                          subject: api.UpdatedSubjectFrontPageData,
                          language: String): Try[api.SubjectPageData] = {
      subjectPageRepository.withId(id) match {
        case Some(existingSubject) =>
          for {
            domainSubject <- ConverterService.toDomainSubjectPage(existingSubject, subject)
            subjectPage <- subjectPageRepository.updateSubjectPage(domainSubject)
            converted <- ConverterService.toApiSubjectPage(subjectPage, language, true)
          } yield converted
        case None =>
          newFromUpdatedSubjectPage(subject) match {
            case None                 => Failure(ValidationException(s"Subjectpage can't be converted to NewSubjectFrontPageData"))
            case Some(newSubjectPage) => updateSubjectPage(id, newSubjectPage, language)
          }
      }
    }

    private def newFromUpdatedSubjectPage(
        updatedSubjectPage: api.UpdatedSubjectFrontPageData): Option[api.NewSubjectFrontPageData] = {
      for {
        name <- updatedSubjectPage.name
        layout <- updatedSubjectPage.layout
        banner <- updatedSubjectPage.banner
        about <- updatedSubjectPage.about
        metaDescription <- updatedSubjectPage.metaDescription
      } yield
        api.NewSubjectFrontPageData(
          name = name,
          filters = updatedSubjectPage.filters,
          externalId = updatedSubjectPage.externalId,
          layout = layout,
          twitter = updatedSubjectPage.twitter,
          facebook = updatedSubjectPage.facebook,
          banner = banner,
          about = about,
          metaDescription = metaDescription,
          topical = updatedSubjectPage.topical,
          mostRead = updatedSubjectPage.mostRead,
          editorsChoices = updatedSubjectPage.editorsChoices,
          latestContent = updatedSubjectPage.latestContent,
          goTo = updatedSubjectPage.goTo
        )
    }

    def updateFrontPage(page: api.FrontPageData): Try[api.FrontPageData] = {
      val domainFrontpage = ConverterService.toDomainFrontPage(page)
      frontPageRepository
        .newFrontPage(domainFrontpage)
        .map(ConverterService.toApiFrontPage)
    }

    def updateFilmFrontPage(page: api.NewOrUpdatedFilmFrontPageData): Try[api.FilmFrontPageData] = {
      val domainFilmFrontPageT = ConverterService.toDomainFilmFrontPage(page)
      for {
        domainFilmFrontPage <- domainFilmFrontPageT
        filmFrontPage <- filmFrontPageRepository.newFilmFrontPage(domainFilmFrontPage)
      } yield ConverterService.toApiFilmFrontPage(filmFrontPage, None)
    }
  }

}

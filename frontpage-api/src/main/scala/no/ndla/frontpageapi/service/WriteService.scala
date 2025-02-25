/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import no.ndla.common.model.api.FrontPageDTO
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.api
import no.ndla.frontpageapi.model.domain.Errors.{SubjectPageNotFoundException, ValidationException}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: SubjectPageRepository with FrontPageRepository with FilmFrontPageRepository with Props with ConverterService =>
  val writeService: WriteService

  class WriteService {

    def newSubjectPage(subject: api.NewSubjectFrontPageDataDTO): Try[api.SubjectPageDataDTO] = {
      for {
        convertedSubject <- ConverterService.toDomainSubjectPage(subject)
        subjectPage      <- subjectPageRepository.newSubjectPage(convertedSubject, subject.externalId.getOrElse(""))
        converted        <- ConverterService.toApiSubjectPage(subjectPage, props.DefaultLanguage, fallback = true)
      } yield converted
    }

    def updateSubjectPage(
        id: Long,
        subject: api.NewSubjectFrontPageDataDTO,
        language: String
    ): Try[api.SubjectPageDataDTO] = {
      subjectPageRepository.exists(id) match {
        case Success(exists) if exists =>
          for {
            domainSubject <- ConverterService.toDomainSubjectPage(id, subject)
            subjectPage   <- subjectPageRepository.updateSubjectPage(domainSubject)
            converted     <- ConverterService.toApiSubjectPage(subjectPage, language, fallback = true)
          } yield converted
        case Success(_) =>
          Failure(SubjectPageNotFoundException(id))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateSubjectPage(
        id: Long,
        subject: api.UpdatedSubjectFrontPageDataDTO,
        language: String,
        fallback: Boolean
    ): Try[api.SubjectPageDataDTO] = {
      subjectPageRepository.withId(id) match {
        case Failure(ex) => Failure(ex)
        case Success(Some(existingSubject)) =>
          for {
            domainSubject <- ConverterService.toDomainSubjectPage(existingSubject, subject)
            subjectPage   <- subjectPageRepository.updateSubjectPage(domainSubject)
            converted     <- ConverterService.toApiSubjectPage(subjectPage, language, fallback)
          } yield converted
        case Success(None) =>
          newFromUpdatedSubjectPage(subject) match {
            case None => Failure(ValidationException(s"Subjectpage can't be converted to NewSubjectFrontPageData"))
            case Some(newSubjectPage) => updateSubjectPage(id, newSubjectPage, language)
          }
      }
    }

    private def newFromUpdatedSubjectPage(
        updatedSubjectPage: api.UpdatedSubjectFrontPageDataDTO
    ): Option[api.NewSubjectFrontPageDataDTO] = {
      for {
        name            <- updatedSubjectPage.name
        banner          <- updatedSubjectPage.banner
        about           <- updatedSubjectPage.about
        metaDescription <- updatedSubjectPage.metaDescription
      } yield api.NewSubjectFrontPageDataDTO(
        name = name,
        externalId = updatedSubjectPage.externalId,
        banner = banner,
        about = about,
        metaDescription = metaDescription,
        editorsChoices = updatedSubjectPage.editorsChoices,
        connectedTo = updatedSubjectPage.connectedTo,
        buildsOn = updatedSubjectPage.buildsOn,
        leadsTo = updatedSubjectPage.leadsTo
      )
    }

    def createFrontPage(page: FrontPageDTO): Try[FrontPageDTO] = {
      for {
        domainFrontpage <- Try(ConverterService.toDomainFrontPage(page))
        inserted        <- frontPageRepository.newFrontPage(domainFrontpage)
        api             <- Try(ConverterService.toApiFrontPage(inserted))
      } yield api
    }

    def updateFilmFrontPage(page: api.NewOrUpdatedFilmFrontPageDataDTO): Try[api.FilmFrontPageDataDTO] = {
      val domainFilmFrontPageT = ConverterService.toDomainFilmFrontPage(page)
      for {
        domainFilmFrontPage <- domainFilmFrontPageT
        filmFrontPage       <- filmFrontPageRepository.newFilmFrontPage(domainFilmFrontPage)
      } yield ConverterService.toApiFilmFrontPage(filmFrontPage, None)
    }
  }

}

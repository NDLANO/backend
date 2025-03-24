/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.service

import no.ndla.common.errors.{NotFoundException, ValidationException}
import no.ndla.common.model.api.FrontPageDTO
import no.ndla.common.model.api.frontpage.SubjectPageDTO
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.api
import no.ndla.frontpageapi.model.domain.Errors.{OperationNotAllowedException, SubjectPageNotFoundException}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.language.Language

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: SubjectPageRepository & FrontPageRepository & FilmFrontPageRepository & Props & ConverterService =>
  val writeService: WriteService

  class WriteService {

    def newSubjectPage(subject: api.NewSubjectPageDTO): Try[SubjectPageDTO] = {
      for {
        convertedSubject <- ConverterService.toDomainSubjectPage(subject)
        subjectPage      <- subjectPageRepository.newSubjectPage(convertedSubject, subject.externalId.getOrElse(""))
        converted        <- ConverterService.toApiSubjectPage(subjectPage, props.DefaultLanguage, fallback = true)
      } yield converted
    }

    def updateSubjectPage(
        id: Long,
        subject: api.NewSubjectPageDTO,
        language: String
    ): Try[SubjectPageDTO] = {
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
        subject: api.UpdatedSubjectPageDTO,
        language: String,
        fallback: Boolean
    ): Try[SubjectPageDTO] = {
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
            case None =>
              Failure(ValidationException("subjectpage", s"Subjectpage can't be converted to NewSubjectFrontPageData"))
            case Some(newSubjectPage) => updateSubjectPage(id, newSubjectPage, language)
          }
      }
    }

    private def newFromUpdatedSubjectPage(
        updatedSubjectPage: api.UpdatedSubjectPageDTO
    ): Option[api.NewSubjectPageDTO] = {
      for {
        name            <- updatedSubjectPage.name
        banner          <- updatedSubjectPage.banner
        about           <- updatedSubjectPage.about
        metaDescription <- updatedSubjectPage.metaDescription
      } yield api.NewSubjectPageDTO(
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

    def updateFilmFrontPage(page: api.NewOrUpdatedFilmFrontPageDTO): Try[api.FilmFrontPageDTO] = {
      val domainFilmFrontPageT = ConverterService.toDomainFilmFrontPage(page)
      for {
        domainFilmFrontPage <- domainFilmFrontPageT
        filmFrontPage       <- filmFrontPageRepository.newFilmFrontPage(domainFilmFrontPage)
      } yield ConverterService.toApiFilmFrontPage(filmFrontPage, None)
    }

    def deleteSubjectPageLanguage(id: Long, language: String): Try[SubjectPageDTO] = {
      subjectPageRepository.withId(id) match {
        case Success(Some(subjectPage)) =>
          subjectPage.about.size match {
            case 1 => Failure(OperationNotAllowedException("Only one language left"))
            case _ =>
              val about           = subjectPage.about.filter(_.language != language)
              val metaDescription = subjectPage.metaDescription.filter(_.language != language)
              subjectPageRepository
                .updateSubjectPage(
                  subjectPage.copy(
                    about = about,
                    metaDescription = metaDescription
                  )
                )
                .flatMap(ConverterService.toApiSubjectPage(_, Language.NoLanguage, fallback = true))
          }
        case Success(None) => Failure(SubjectPageNotFoundException(id))
        case Failure(ex)   => Failure(ex)
      }
    }

    def deleteFilmFrontPageLanguage(language: String): Try[api.FilmFrontPageDTO] = {
      filmFrontPageRepository.get match {
        case Some(page) =>
          page.about.size match {
            case 1 => Failure(OperationNotAllowedException("Only one language left"))
            case _ =>
              filmFrontPageRepository
                .update(
                  page.copy(
                    about = page.about.filter(_.language != language),
                    movieThemes = page.movieThemes.map(movieTheme =>
                      movieTheme.copy(name = movieTheme.name.filter(_.language != language))
                    )
                  )
                )
                .map(ConverterService.toApiFilmFrontPage(_, None))
          }
        case None => Failure(NotFoundException("The film front page was not found"))
      }
    }
  }

}

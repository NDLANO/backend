/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import java.util.Date

import no.ndla.draftapi.auth.{Role, User}
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.repository.{AgreementRepository, ConceptRepository, DraftRepository}
import no.ndla.draftapi.service.search.{AgreementIndexService, ArticleIndexService, ConceptIndexService}
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.draftapi.model.domain.ArticleStatus.{PUBLISHED, QUEUED_FOR_PUBLISHING}
import no.ndla.draftapi.model.domain.Language.UnknownLanguage

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: DraftRepository
    with ConceptRepository
    with AgreementRepository
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with AgreementIndexService
    with ConceptIndexService
    with Clock
    with User
    with ReadService
    with ArticleApiClient
    with Role
    with ArticleApiClient =>
  val writeService: WriteService

  class WriteService {

    def updateAgreement(agreementId: Long, updatedAgreement: api.UpdatedAgreement): Try[api.Agreement] = {
      agreementRepository.withId(agreementId) match {
        case None => Failure(NotFoundException(s"Agreement with id $agreementId does not exist"))
        case Some(existing) =>
          val toUpdate = existing.copy(
            title = updatedAgreement.title.getOrElse(existing.title),
            content = updatedAgreement.content.getOrElse(existing.content),
            copyright =
              updatedAgreement.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
            updated = clock.now(),
            updatedBy = authUser.userOrClientId()
          )

          val dateErrors = updatedAgreement.copyright
            .map(updatedCopyright => contentValidator.validateDates(updatedCopyright))
            .getOrElse(Seq.empty)

          for {
            _ <- contentValidator.validateAgreement(toUpdate, preExistingErrors = dateErrors)
            agreement <- agreementRepository.update(toUpdate)
            _ <- agreementIndexService.indexDocument(agreement)
          } yield converterService.toApiAgreement(agreement)
      }
    }

    def newAgreement(newAgreement: api.NewAgreement): Try[api.Agreement] = {
      val apiErrors = contentValidator.validateDates(newAgreement.copyright)

      val domainAgreement = converterService.toDomainAgreement(newAgreement)
      contentValidator.validateAgreement(domainAgreement, preExistingErrors = apiErrors) match {
        case Success(_) =>
          val agreement = agreementRepository.insert(domainAgreement)
          agreementIndexService.indexDocument(agreement)
          Success(converterService.toApiAgreement(agreement))
        case Failure(exception) => Failure(exception)
      }
    }

    def newArticle(newArticle: api.NewArticle,
                   externalId: Option[String],
                   externalSubjectIds: Seq[String],
                   oldNdlaCreatedDate: Option[Date],
                   oldNdlaUpdatedDate: Option[Date]): Try[api.Article] = {
      val insertNewArticleFunction = externalId match {
        case None => draftRepository.insert _
        case Some(nid) =>
          (a: domain.Article) =>
            draftRepository.insertWithExternalId(a, nid, externalSubjectIds)
      }
      for {
        domainArticle <- converterService.toDomainArticle(newArticle, externalId, oldNdlaCreatedDate, oldNdlaUpdatedDate)
        _ <- contentValidator.validateArticle(domainArticle, allowUnknownLanguage = false)
        insertedArticle <- Try(insertNewArticleFunction(domainArticle))
        _ <- articleIndexService.indexDocument(insertedArticle)
        apiArticle <- converterService.toApiArticle(insertedArticle, newArticle.language)
      } yield apiArticle
    }

    private def updateArticle(toUpdate: domain.Article,
                              externalId: Option[String] = None,
                              externalSubjectIds: Seq[String] = Seq.empty,
                              isImported: Boolean = false): Try[domain.Article] = {
      val updateFunc = externalId match {
        case None =>
          (a: domain.Article) =>
            draftRepository.update(a, isImported = isImported)
        case Some(nid) =>
          (a: domain.Article) =>
            draftRepository.updateWithExternalIds(a, nid, externalSubjectIds)
      }

      for {
        _ <- contentValidator.validateArticle(toUpdate, allowUnknownLanguage = true)
        domainArticle <- updateFunc(toUpdate)
        _ <- articleIndexService.indexDocument(domainArticle)
      } yield domainArticle
    }

    def updateArticle(articleId: Long,
                      updatedApiArticle: api.UpdatedArticle,
                      externalId: Option[String],
                      externalSubjectIds: Seq[String],
                      oldNdlaCreatedDate: Option[Date],
                      oldNdlaUpdatedDate: Option[Date]): Try[api.Article] = {
      draftRepository.withId(articleId) match {
        case Some(existing) if existing.status.contains(QUEUED_FOR_PUBLISHING) && !authRole.hasPublishPermission() =>
          Failure(
            new OperationNotAllowedException(
              "This article is marked for publishing and it cannot be updated until it is published"))
        case Some(existing) =>
          for {
            domainArticle <- converterService.toDomainArticle(existing, updatedApiArticle, externalId.isDefined, oldNdlaUpdatedDate)
            updatedArticle <- updateArticle(domainArticle,
                                            externalId,
                                            externalSubjectIds,
                                            isImported = externalId.isDefined)
            apiArticle <- converterService.toApiArticle(readService.addUrlsOnEmbedResources(updatedArticle),
              updatedApiArticle.language.getOrElse(UnknownLanguage))
          } yield apiArticle

        case None if draftRepository.exists(articleId) =>
          for {
            article <- converterService.toDomainArticle(articleId, updatedApiArticle, externalId.isDefined, oldNdlaCreatedDate, oldNdlaUpdatedDate)
            updatedArticle <- updateArticle(article, externalId, externalSubjectIds)
            apiArticle <- converterService.toApiArticle(readService.addUrlsOnEmbedResources(updatedArticle),
              updatedApiArticle.language.getOrElse(UnknownLanguage))
          } yield apiArticle
        case None => Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
    }

    def queueArticleForPublish(id: Long, isImported: Boolean = false): Try[ArticleStatus] = {
      draftRepository.withId(id) match {
        case Some(a) =>
          contentValidator.validateArticleApiArticle(id) match {
            case Success(_) =>
              val newStatus = a.status.filterNot(_ == PUBLISHED) + QUEUED_FOR_PUBLISHING
              draftRepository
                .update(a.copy(status = newStatus), isImported = isImported)
                .map(a => converterService.toApiStatus(a.status))
            case Failure(ex) => Failure(ex)
          }
        case None => Failure(NotFoundException(s"The article with id $id does not exist"))
      }
    }

    def publishArticle(id: Long, isImported: Boolean = false): Try[domain.Article] = {
      draftRepository.withId(id) match {
        case Some(article) if article.status.contains(QUEUED_FOR_PUBLISHING) =>
          articleApiClient.updateArticle(id, converterService.toArticleApiArticle(article)) match {
            case Success(_) =>
              updateArticle(article.copy(status = article.status.filter(_ != QUEUED_FOR_PUBLISHING) + PUBLISHED),
                isImported = isImported)
            case Failure(ex) => Failure(ex)
          }
        case Some(_) =>
          Failure(new ArticleStatusException(s"Article with id $id is not marked for publishing"))
        case None =>
          Failure(NotFoundException(s"Article with id $id does not exist"))
      }
    }

    def publishArticles(): ArticlePublishReport = {
      val articlesToPublish = readService.articlesWithStatus(QUEUED_FOR_PUBLISHING)

      articlesToPublish.foldLeft(ArticlePublishReport(Seq.empty, Seq.empty))((result, curr) => {
        publishArticle(curr) match {
          case Success(_)  => result.addSuccessful(curr)
          case Failure(ex) => result.addFailed(FailedArticlePublish(curr, ex.getMessage))
        }
      })
    }

    def deleteArticle(id: Long): Try[api.ContentId] = {
      draftRepository
        .delete(id)
        .flatMap(articleIndexService.deleteDocument)
        .map(api.ContentId)
    }

    def publishConcept(id: Long): Try[domain.Concept] = {
      conceptRepository.withId(id) match {
        case Some(concept) =>
          articleApiClient.updateConcept(id, converterService.toArticleApiConcept(concept)) match {
            case Success(_)  => Success(concept)
            case Failure(ex) => Failure(ex)
          }
        case None => Failure(NotFoundException(s"Article with id $id does not exist"))
      }
    }

    def deleteConcept(id: Long): Try[api.ContentId] = {
      conceptRepository
        .delete(id)
        .flatMap(conceptIndexService.deleteDocument)
        .map(api.ContentId)
    }

    def newConcept(newConcept: NewConcept, externalId: String): Try[api.Concept] = {
      for {
        concept <- converterService.toDomainConcept(newConcept)
        _ <- importValidator.validate(concept)
        persistedConcept <- Try(conceptRepository.insertWithExternalId(concept, externalId))
        _ <- conceptIndexService.indexDocument(concept)
      } yield converterService.toApiConcept(persistedConcept, newConcept.language)
    }

    private def updateConcept(toUpdate: domain.Concept, externalId: Option[String] = None): Try[domain.Concept] = {
      val updateFunc = externalId match {
        case None => conceptRepository.update _
        case Some(nid) =>
          (a: domain.Concept) =>
            conceptRepository.updateWithExternalId(a, nid)
      }

      for {
        _ <- contentValidator.validate(toUpdate, allowUnknownLanguage = true)
        domainConcept <- updateFunc(toUpdate)
        _ <- conceptIndexService.indexDocument(domainConcept)
      } yield domainConcept
    }

    def updateConcept(id: Long, updatedConcept: api.UpdatedConcept, externalId: Option[String]): Try[api.Concept] = {
      conceptRepository.withId(id) match {
        case Some(concept) =>
          val domainConcept = converterService.toDomainConcept(concept, updatedConcept)
          updateConcept(domainConcept, externalId)
            .map(x => converterService.toApiConcept(x, updatedConcept.language))
        case None if conceptRepository.exists(id) =>
          val concept = converterService.toDomainConcept(id, updatedConcept)
          updateConcept(concept, externalId)
            .map(concept => converterService.toApiConcept(concept, updatedConcept.language))
        case None => Failure(NotFoundException(s"Concept with id $id does not exist"))
      }
    }

    private[service] def mergeLanguageFields[A <: LanguageField[_]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.isEmpty)
    }

    def newEmptyArticle(externalId: String, externalSubjectIds: Seq[String]): Try[Long] = {
      articleApiClient
        .allocateArticleId(Some(externalId), externalSubjectIds)
        .flatMap(id => draftRepository.newEmptyArticle(id, externalId, externalSubjectIds))
    }

    def newEmptyConcept(externalId: String): Try[Long] = {
      articleApiClient
        .allocateConceptId(Some(externalId))
        .flatMap(id => conceptRepository.newEmptyConcept(id, externalId))
    }

  }

}

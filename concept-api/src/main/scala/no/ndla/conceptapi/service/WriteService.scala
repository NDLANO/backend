/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.auth.UserInfo
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.domain.ConceptStatus._
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.{ConceptExistsAlreadyException, ConceptMissingIdException, NotFoundException}
import no.ndla.conceptapi.model.domain.{ConceptStatus, Language}
import no.ndla.conceptapi.service.search.{DraftConceptIndexService, PublishedConceptIndexService}
import no.ndla.conceptapi.validation._

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: DraftConceptRepository
    with PublishedConceptRepository
    with ConverterService
    with ContentValidator
    with DraftConceptIndexService
    with PublishedConceptIndexService
    with LazyLogging =>
  val writeService: WriteService

  class WriteService {

    def insertListingImportedConcepts(conceptsWithListingId: Seq[(domain.Concept, Long)],
                                      forceUpdate: Boolean): Seq[Try[domain.Concept]] = {
      conceptsWithListingId.map {
        case (concept, listingId) =>
          val existing = draftConceptRepository.withListingId(listingId).nonEmpty
          if (existing && !forceUpdate) {
            logger.warn(
              s"Concept with listing_id of $listingId already exists, and forceUpdate was not 'true', skipping...")
            Failure(ConceptExistsAlreadyException(s"the concept already exists with listing_id of $listingId."))
          } else if (existing && forceUpdate) {
            draftConceptRepository.updateWithListingId(concept, listingId)
          } else {
            Success(draftConceptRepository.insertwithListingId(concept, listingId))
          }
      }
    }

    def saveImportedConcepts(concepts: Seq[domain.Concept], forceUpdate: Boolean): Seq[Try[domain.Concept]] = {
      concepts.map(concept => {
        concept.id match {
          case Some(id) if draftConceptRepository.exists(id) =>
            if (forceUpdate) {
              val existing = draftConceptRepository.withId(id)
              updateConcept(concept) match {
                case Failure(ex) =>
                  logger.error(s"Could not update concept with id '${concept.id.getOrElse(-1)}' when importing.")
                  Failure(ex)
                case Success(c) =>
                  logger.info(s"Updated concept with id '${c.id.getOrElse(-1)}' successfully during import.")
                  Success(c)
              }
            } else {
              Failure(ConceptExistsAlreadyException("The concept already exists."))
            }
          case None => draftConceptRepository.insertWithId(concept)
        }
      })
    }

    def newConcept(newConcept: api.NewConcept, userInfo: UserInfo): Try[api.Concept] = {
      for {
        concept <- converterService.toDomainConcept(newConcept, userInfo)
        _ <- contentValidator.validateConcept(concept)
        persistedConcept <- Try(draftConceptRepository.insert(concept))
        _ <- draftConceptIndexService.indexDocument(persistedConcept)
        apiC <- converterService.toApiConcept(persistedConcept, newConcept.language, fallback = true)
      } yield apiC
    }

    private def shouldUpdateStatus(existing: domain.Concept, changed: domain.Concept): Boolean = {
      // Function that sets values we don't want to include when comparing concepts to check if we should update status
      val withComparableValues =
        (concept: domain.Concept) =>
          concept.copy(
            revision = None,
            created = new Date(0),
            updated = new Date(0)
        )
      withComparableValues(existing) != withComparableValues(changed)
    }

    private def updateStatusIfNeeded(
        existing: domain.Concept,
        changed: domain.Concept,
        updateStatus: Option[String],
        user: UserInfo
    ) = {
      if (!shouldUpdateStatus(existing, changed) && updateStatus.isEmpty) {
        Success(changed)
      } else {
        val oldStatus = existing.status.current
        val newStatusIfNotDefined = if (oldStatus == PUBLISHED) DRAFT else oldStatus
        val newStatus = updateStatus.flatMap(ConceptStatus.valueOf).getOrElse(newStatusIfNotDefined)

        converterService
          .updateStatus(newStatus, changed, user)
          .attempt
          .unsafeRunSync()
          .toTry
          .flatten
      }
    }

    private def updateConcept(toUpdate: domain.Concept): Try[domain.Concept] = {
      for {
        _ <- contentValidator.validateConcept(toUpdate)
        domainConcept <- draftConceptRepository.update(toUpdate)
        _ <- draftConceptIndexService.indexDocument(domainConcept)
      } yield domainConcept
    }

    def updateConcept(id: Long, updatedConcept: api.UpdatedConcept, userInfo: UserInfo): Try[api.Concept] = {
      draftConceptRepository.withId(id) match {
        case Some(existingConcept) =>
          val domainConcept = converterService.toDomainConcept(existingConcept, updatedConcept, userInfo)

          for {
            withStatus <- updateStatusIfNeeded(existingConcept, domainConcept, updatedConcept.status, userInfo)
            updated <- updateConcept(withStatus)
            converted <- converterService.toApiConcept(updated, updatedConcept.language, fallback = true)
          } yield converted

        case None if draftConceptRepository.exists(id) =>
          val concept = converterService.toDomainConcept(id, updatedConcept, userInfo)
          for {
            updated <- updateConcept(concept)
            converted <- converterService.toApiConcept(updated, updatedConcept.language, fallback = true)
          } yield converted
        case None =>
          Failure(NotFoundException(s"Concept with id $id does not exist"))
      }
    }

    def deleteLanguage(id: Long, language: String, userInfo: UserInfo): Try[api.Concept] = {
      draftConceptRepository.withId(id) match {
        case Some(existingConcept) =>
          existingConcept.title.size match {
            case 1 => Failure(api.OperationNotAllowedException("Only one language left"))
            case _ =>
              val title = existingConcept.title.filter(_.language != language)
              val content = existingConcept.content.filter(_.language != language)
              val newConcept = existingConcept.copy(
                title = title,
                content = content,
              )

              for {
                withStatus <- updateStatusIfNeeded(existingConcept, newConcept, None, userInfo)
                updated <- updateConcept(withStatus)
                converted <- converterService.toApiConcept(updated, domain.Language.AllLanguages, fallback = false)
              } yield converted
          }
        case None => Failure(NotFoundException("Concept does not exist"))
      }

    }

    def updateConceptStatus(status: domain.ConceptStatus.Value, id: Long, user: UserInfo): Try[api.Concept] = {
      draftConceptRepository.withId(id) match {
        case None => Failure(NotFoundException(s"No article with id $id was found"))
        case Some(draft) =>
          for {
            convertedConceptT <- converterService
              .updateStatus(status, draft, user)
              .attempt
              .unsafeRunSync()
              .toTry
            convertedConcept <- convertedConceptT
            updatedConcept <- updateConcept(convertedConcept)
            _ <- draftConceptIndexService.indexDocument(updatedConcept)
            apiConcept <- converterService.toApiConcept(updatedConcept, Language.AllLanguages, fallback = true)
          } yield apiConcept
      }
    }

    def publishConcept(concept: domain.Concept): Try[domain.Concept] = {
      for {
        inserted <- publishedConceptRepository.insertOrUpdate(concept)
        indexed <- publishedConceptIndexService.indexDocument(inserted)
      } yield indexed
    }

    def unpublishConcept(concept: domain.Concept): Try[domain.Concept] = {
      concept.id match {
        case Some(id) =>
          for {
            _ <- publishedConceptRepository.delete(id).map(_ => concept)
            _ <- publishedConceptIndexService.deleteDocument(id)
          } yield concept
        case None => Failure(ConceptMissingIdException("Cannot attempt to unpublish concept without id"))
      }
    }
  }
}

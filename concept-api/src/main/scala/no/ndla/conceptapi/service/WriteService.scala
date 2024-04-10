/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.UpdateWith
import no.ndla.common.model.domain.concept.ConceptStatus.*
import no.ndla.common.model.domain.concept.{ConceptEditorNote, ConceptStatus, Concept as DomainConcept}
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.{ConceptExistsAlreadyException, ConceptMissingIdException, NotFoundException}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.{DraftConceptIndexService, PublishedConceptIndexService}
import no.ndla.conceptapi.validation.*
import no.ndla.language.Language
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: DraftConceptRepository
    with PublishedConceptRepository
    with ConverterService
    with ContentValidator
    with DraftConceptIndexService
    with PublishedConceptIndexService
    with StrictLogging
    with Clock =>
  val writeService: WriteService

  class WriteService {

    def insertListingImportedConcepts(
        conceptsWithListingId: Seq[(DomainConcept, Long)],
        forceUpdate: Boolean
    ): Seq[Try[DomainConcept]] = {
      conceptsWithListingId.map { case (concept, listingId) =>
        val existing = draftConceptRepository.withListingId(listingId).nonEmpty
        if (existing && !forceUpdate) {
          logger.warn(
            s"Concept with listing_id of $listingId already exists, and forceUpdate was not 'true', skipping..."
          )
          Failure(ConceptExistsAlreadyException(s"the concept already exists with listing_id of $listingId."))
        } else if (existing && forceUpdate) {
          draftConceptRepository.updateWithListingId(concept, listingId)
        } else {
          Success(draftConceptRepository.insertwithListingId(concept, listingId))
        }
      }
    }

    def saveImportedConcepts(concepts: Seq[DomainConcept], forceUpdate: Boolean): Seq[Try[DomainConcept]] = {
      concepts.map(concept => {
        concept.id match {
          case Some(id) if draftConceptRepository.exists(id) =>
            if (forceUpdate) {
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
          case _ => draftConceptRepository.insertWithId(concept)
        }
      })
    }

    def newConcept(newConcept: api.NewConcept, userInfo: TokenUser): Try[api.Concept] = {
      for {
        concept          <- converterService.toDomainConcept(newConcept, userInfo)
        _                <- contentValidator.validateConcept(concept)
        persistedConcept <- Try(draftConceptRepository.insert(concept))
        _                <- draftConceptIndexService.indexDocument(persistedConcept)
        apiC <- converterService.toApiConcept(persistedConcept, newConcept.language, fallback = true, Some(userInfo))
      } yield apiC
    }

    private def shouldUpdateStatus(existing: DomainConcept, changed: DomainConcept): Boolean = {
      // Function that sets values we don't want to include when comparing concepts to check if we should update status
      val withComparableValues =
        (concept: DomainConcept) =>
          concept.copy(
            revision = None,
            created = NDLADate.fromUnixTime(0),
            updated = NDLADate.fromUnixTime(0)
          )
      withComparableValues(existing) != withComparableValues(changed)
    }

    private def updateStatusIfNeeded(
        existing: DomainConcept,
        changed: DomainConcept,
        updateStatus: Option[String],
        user: TokenUser
    ): Try[DomainConcept] = {
      if (!shouldUpdateStatus(existing, changed) && updateStatus.isEmpty) {
        Success(changed)
      } else {
        val oldStatus             = existing.status.current
        val newStatusIfNotDefined = if (oldStatus == PUBLISHED) IN_PROGRESS else oldStatus
        val newStatus             = updateStatus.flatMap(ConceptStatus.valueOf).getOrElse(newStatusIfNotDefined)

        converterService.updateStatus(newStatus, changed, user)
      }
    }

    private def shouldUpdateNotes(existing: DomainConcept, changed: DomainConcept): Boolean = {
      // Function that sets values we don't want to include when comparing concepts to check if we should update notes
      val withComparableValues =
        (concept: DomainConcept) =>
          concept.copy(
            revision = None,
            created = NDLADate.fromUnixTime(0),
            updated = NDLADate.fromUnixTime(0),
            responsible = None,
            updatedBy = Seq.empty
          )
      withComparableValues(existing) != withComparableValues(changed)
    }

    private def updateNotes(
        old: DomainConcept,
        updated: api.UpdatedConcept,
        changed: DomainConcept,
        user: TokenUser
    ): DomainConcept = {
      val isNewLanguage =
        !old.supportedLanguages.contains(updated.language) && changed.supportedLanguages.contains(updated.language)
      val dataChanged = shouldUpdateNotes(old, changed);

      val newEditorNote =
        if (isNewLanguage) Seq(s"New language '${updated.language}' added")
        else if (dataChanged) Seq(s"Updated ${old.conceptType}")
        else Seq.empty

      val changedResponsibleNote =
        updated.responsibleId match {
          case UpdateWith(newId) if !old.responsible.map(_.responsibleId).contains(newId) =>
            Seq("Responsible changed")
          case _ => Seq.empty
        }
      val allNewNotes = newEditorNote ++ changedResponsibleNote

      changed.copy(editorNotes =
        changed.editorNotes ++ allNewNotes.map(ConceptEditorNote(_, user.id, changed.status, clock.now()))
      )
    }

    private def updateConcept(toUpdate: DomainConcept): Try[DomainConcept] = {
      for {
        _             <- contentValidator.validateConcept(toUpdate)
        domainConcept <- draftConceptRepository.update(toUpdate)
        _             <- draftConceptIndexService.indexDocument(domainConcept)
      } yield domainConcept
    }

    def updateConcept(id: Long, updatedConcept: api.UpdatedConcept, userInfo: TokenUser): Try[api.Concept] = {
      draftConceptRepository.withId(id) match {
        case Some(existingConcept) =>
          for {
            domainConcept <- converterService.toDomainConcept(existingConcept, updatedConcept, userInfo)
            withStatus    <- updateStatusIfNeeded(existingConcept, domainConcept, updatedConcept.status, userInfo)
            withNotes = updateNotes(existingConcept, updatedConcept, withStatus, userInfo)
            updated <- updateConcept(withNotes)
            converted <- converterService.toApiConcept(
              updated,
              updatedConcept.language,
              fallback = true,
              Some(userInfo)
            )
          } yield converted

        case None if draftConceptRepository.exists(id) =>
          val concept = converterService.toDomainConcept(id, updatedConcept, userInfo)
          for {
            updated <- updateConcept(concept)
            converted <- converterService.toApiConcept(
              updated,
              updatedConcept.language,
              fallback = true,
              Some(userInfo)
            )
          } yield converted
        case None =>
          Failure(NotFoundException(s"Concept with id $id does not exist"))
      }
    }

    def deleteLanguage(id: Long, language: String, userInfo: TokenUser): Try[api.Concept] = {
      draftConceptRepository.withId(id) match {
        case Some(existingConcept) =>
          existingConcept.title.size match {
            case 1 => Failure(api.OperationNotAllowedException("Only one language left"))
            case _ =>
              val title         = existingConcept.title.filter(_.language != language)
              val content       = existingConcept.content.filter(_.language != language)
              val tags          = existingConcept.tags.filter(_.language != language)
              val metaImage     = existingConcept.metaImage.filter(_.language != language)
              val visualElement = existingConcept.visualElement.filter(_.language != language)

              val newConcept = existingConcept.copy(
                title = title,
                content = content,
                tags = tags,
                metaImage = metaImage,
                visualElement = visualElement
              )

              for {
                withStatus <- updateStatusIfNeeded(existingConcept, newConcept, None, userInfo)
                conceptWithUpdatedNotes = withStatus.copy(editorNotes =
                  withStatus.editorNotes ++ Seq(
                    ConceptEditorNote(
                      s"Deleted language '$language'.",
                      userInfo.id,
                      withStatus.status,
                      clock.now()
                    )
                  )
                )
                updated <- updateConcept(conceptWithUpdatedNotes)
                converted <- converterService.toApiConcept(
                  updated,
                  Language.AllLanguages,
                  fallback = false,
                  Some(userInfo)
                )
              } yield converted
          }
        case None => Failure(NotFoundException("Concept does not exist"))
      }

    }

    def updateConceptStatus(status: ConceptStatus, id: Long, user: TokenUser): Try[api.Concept] = {
      draftConceptRepository.withId(id) match {
        case None => Failure(NotFoundException(s"No article with id $id was found"))
        case Some(draft) =>
          for {
            convertedConcept <- converterService.updateStatus(status, draft, user)
            updatedConcept   <- updateConcept(convertedConcept)
            _                <- draftConceptIndexService.indexDocument(updatedConcept)
            apiConcept <- converterService.toApiConcept(
              updatedConcept,
              Language.AllLanguages,
              fallback = true,
              Some(user)
            )
          } yield apiConcept
      }
    }

    def publishConcept(concept: DomainConcept): Try[DomainConcept] = {
      for {
        inserted <- publishedConceptRepository.insertOrUpdate(concept)
        indexed  <- publishedConceptIndexService.indexDocument(inserted)
      } yield indexed
    }

    def unpublishConcept(concept: DomainConcept): Try[DomainConcept] = {
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

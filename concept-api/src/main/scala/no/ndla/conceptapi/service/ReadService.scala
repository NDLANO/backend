/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.NotFoundException
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.language.Language
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: DraftConceptRepository & PublishedConceptRepository & ConverterService =>
  val readService: ReadService

  class ReadService {

    def conceptWithId(id: Long, language: String, fallback: Boolean, user: Option[TokenUser]): Try[api.ConceptDTO] =
      draftConceptRepository.withId(id).map(converterService.addUrlOnVisualElement) match {
        case Some(concept) =>
          converterService.toApiConcept(concept, language, fallback, user)
        case None =>
          Failure(NotFoundException(s"Concept with id $id was not found with language '$language' in database."))
      }

    def publishedConceptWithId(
        id: Long,
        language: String,
        fallback: Boolean,
        user: Option[TokenUser]
    ): Try[api.ConceptDTO] =
      publishedConceptRepository.withId(id).map(converterService.addUrlOnVisualElement) match {
        case Some(concept) =>
          converterService.toApiConcept(concept, language, fallback, user)
        case None =>
          Failure(NotFoundException(s"A concept with id $id was not found with language '$language'."))
      }

    def allSubjects(draft: Boolean = false): Try[Set[String]] = {
      if (draft) {
        val subjectIds = draftConceptRepository.allSubjectIds
        if (subjectIds.nonEmpty) Success(subjectIds) else Failure(NotFoundException("Could not find any subjects"))
      } else {
        val subjectIds = publishedConceptRepository.allSubjectIds
        if (subjectIds.nonEmpty) Success(subjectIds) else Failure(NotFoundException("Could not find any subjects"))
      }
    }

    def allTagsFromConcepts(language: String, fallback: Boolean): List[String] = {
      val allConceptTags = publishedConceptRepository.everyTagFromEveryConcept
      (if (fallback || language == Language.AllLanguages) {
         allConceptTags.flatMap(t => {
           Language.findByLanguageOrBestEffort(t, language)
         })
       } else {
         allConceptTags.flatMap(_.filter(_.language == language))
       }).flatMap(_.tags).distinct
    }

    def allTagsFromDraftConcepts(language: String, fallback: Boolean): List[String] = {
      val allConceptTags = draftConceptRepository.everyTagFromEveryConcept
      (if (fallback || language == Language.AllLanguages) {
         allConceptTags.flatMap(t => {
           Language.findByLanguageOrBestEffort(t, language)
         })
       } else {
         allConceptTags.flatMap(_.filter(_.language == language))
       }).flatMap(_.tags).distinct
    }

    def getAllTags(input: String, pageSize: Int, offset: Int, language: String): api.TagsSearchResultDTO = {
      val (tags, tagsCount) = draftConceptRepository.getTags(input, pageSize, (offset - 1) * pageSize, language)
      converterService.toApiConceptTags(tags, tagsCount, pageSize, offset, language)
    }

    def getPublishedConceptDomainDump(pageNo: Int, pageSize: Int): api.ConceptDomainDump = {
      val (safePageNo, safePageSize) = (math.max(pageNo, 1), math.max(pageSize, 0))
      val results = publishedConceptRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)

      api.ConceptDomainDump(publishedConceptRepository.conceptCount, pageNo, pageSize, results)
    }

    def getDraftConceptDomainDump(pageNo: Int, pageSize: Int): api.ConceptDomainDump = {
      val (safePageNo, safePageSize) = (math.max(pageNo, 1), math.max(pageSize, 0))
      val results                    = draftConceptRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)

      api.ConceptDomainDump(draftConceptRepository.conceptCount, pageNo, pageSize, results)
    }
  }
}

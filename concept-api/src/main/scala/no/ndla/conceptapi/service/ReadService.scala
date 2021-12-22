/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import io.lemonlabs.uri.{Path, Url}
import no.ndla.conceptapi.ConceptApiProperties.externalApiUrls
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.NotFoundException
import no.ndla.conceptapi.model.domain.{Concept, Language}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.validation.{ResourceType, TagAttributes}
import org.jsoup.nodes.Element

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: DraftConceptRepository with PublishedConceptRepository with ConverterService =>
  val readService: ReadService

  class ReadService {

    def conceptWithId(id: Long, language: String, fallback: Boolean): Try[api.Concept] =
      draftConceptRepository.withId(id).map(addUrlOnVisualElement) match {
        case Some(concept) =>
          converterService.toApiConcept(concept, language, fallback)
        case None =>
          Failure(NotFoundException(s"Concept with id $id was not found with language '$language' in database."))
      }

    def publishedConceptWithId(id: Long, language: String, fallback: Boolean): Try[api.Concept] =
      publishedConceptRepository.withId(id).map(addUrlOnVisualElement) match {
        case Some(concept) =>
          converterService.toApiConcept(concept, language, fallback)
        case None =>
          Failure(NotFoundException(s"A concept with id $id was not found with language '$language'."))
      }

    def addUrlOnVisualElement(concept: Concept): Concept = {
      val visualElementWithUrls =
        concept.visualElement.map(visual => visual.copy(visualElement = addUrlOnElement(visual.visualElement)))
      concept.copy(visualElement = visualElementWithUrls)
    }

    private[service] def addUrlOnElement(content: String): String = {
      val doc = stringToJsoupDocument(content)

      val embedTags = doc.select(s"$ResourceHtmlEmbedTag").asScala.toList
      embedTags.foreach(addUrlOnEmbedTag)
      jsoupDocumentToString(doc)
    }

    private def addUrlOnEmbedTag(embedTag: Element): Unit = {
      val typeAndPathOption = embedTag.attr(TagAttributes.DataResource.toString) match {
        case resourceType
            if resourceType == ResourceType.H5P.toString
              && embedTag.hasAttr(TagAttributes.DataPath.toString) =>
          val path = embedTag.attr(TagAttributes.DataPath.toString)
          Some((resourceType, path))

        case resourceType if embedTag.hasAttr(TagAttributes.DataResource_Id.toString) =>
          val id = embedTag.attr(TagAttributes.DataResource_Id.toString)
          Some((resourceType, id))
        case _ =>
          None
      }

      typeAndPathOption match {
        case Some((resourceType, path)) =>
          val baseUrl = Url.parse(externalApiUrls(resourceType))
          val pathParts = Path.parse(path).parts

          embedTag.attr(
            s"${TagAttributes.DataUrl}",
            baseUrl.addPathParts(pathParts).toString
          )
        case _ =>
      }
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

    def getAllTags(input: String, pageSize: Int, offset: Int, language: String): api.TagsSearchResult = {
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
      val results = draftConceptRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)

      api.ConceptDomainDump(draftConceptRepository.conceptCount, pageNo, pageSize, results)
    }
  }
}

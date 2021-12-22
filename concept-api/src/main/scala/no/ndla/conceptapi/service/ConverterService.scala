/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.repository.DraftConceptRepository
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.search
import no.ndla.conceptapi.model.domain.Language._
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.NotFoundException
import no.ndla.conceptapi.model.domain.{ConceptStatus, LanguageField, Status}
import no.ndla.mapping.License.getLicense
import no.ndla.conceptapi.ConceptApiProperties._
import no.ndla.conceptapi.auth.UserInfo
import no.ndla.validation.{EmbedTagRules, HtmlTagRules, ResourceType, TagAttributes}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with DraftConceptRepository with StateTransitionRules =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {

    def toApiConcept(concept: domain.Concept, language: String, fallback: Boolean): Try[api.Concept] = {
      val isLanguageNeutral = concept.supportedLanguages.contains(UnknownLanguage.toString) && concept.supportedLanguages.size == 1
      if (concept.supportedLanguages.contains(language) || fallback || isLanguageNeutral || language == AllLanguages) {
        val title = findByLanguageOrBestEffort(concept.title, language)
          .map(toApiConceptTitle)
          .getOrElse(api.ConceptTitle("", UnknownLanguage.toString))
        val content = findByLanguageOrBestEffort(concept.content, language)
          .map(toApiConceptContent)
          .getOrElse(api.ConceptContent("", UnknownLanguage.toString))
        val metaImage = findByLanguageOrBestEffort(concept.metaImage, language)
          .map(toApiMetaImage)
          .getOrElse(api.ConceptMetaImage("", "", UnknownLanguage.toString))

        val tags = findByLanguageOrBestEffort(concept.tags, language).map(toApiTags)

        val visualElement = findByLanguageOrBestEffort(concept.visualElement, language).map(toApiVisualElement)

        Success(
          api.Concept(
            id = concept.id.get,
            revision = concept.revision.getOrElse(-1),
            title = Some(title),
            content = Some(content),
            copyright = concept.copyright.map(toApiCopyright),
            source = concept.source,
            metaImage = Some(metaImage),
            tags = tags,
            subjectIds = if (concept.subjectIds.isEmpty) None else Some(concept.subjectIds),
            created = concept.created,
            updated = concept.updated,
            updatedBy = if (concept.updatedBy.isEmpty) None else Some(concept.updatedBy),
            supportedLanguages = concept.supportedLanguages,
            articleIds = concept.articleIds,
            status = toApiStatus(concept.status),
            visualElement = visualElement
          )
        )
      } else {
        Failure(
          NotFoundException(s"The concept with id ${concept.id.getOrElse(-1)} and language '$language' was not found.",
                            concept.supportedLanguages.toSeq))
      }
    }

    def toApiStatus(status: domain.Status) = {
      api.Status(
        current = status.current.toString,
        other = status.other.map(_.toString).toSeq
      )
    }

    def toApiTags(tags: domain.ConceptTags) = {
      api.ConceptTags(
        tags.tags,
        tags.language
      )
    }

    def toApiCopyright(copyright: domain.Copyright): api.Copyright = {
      api.Copyright(
        copyright.license.map(toApiLicense),
        copyright.origin,
        copyright.creators.map(toApiAuthor),
        copyright.processors.map(toApiAuthor),
        copyright.rightsholders.map(toApiAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toApiLicense(shortLicense: String): api.License = {
      getLicense(shortLicense)
        .map(l => api.License(l.license.toString, Option(l.description), l.url))
        .getOrElse(api.License("unknown", None, None))
    }

    def toApiAuthor(author: domain.Author): api.Author =
      api.Author(author.`type`, author.name)

    def toApiConceptTitle(title: domain.ConceptTitle): api.ConceptTitle =
      api.ConceptTitle(title.title, title.language)

    def toApiConceptContent(title: domain.ConceptContent): api.ConceptContent =
      api.ConceptContent(title.content, title.language)

    def toApiMetaImage(metaImage: domain.ConceptMetaImage): api.ConceptMetaImage =
      api.ConceptMetaImage(s"${externalApiUrls("raw-image")}/${metaImage.imageId}",
                           metaImage.altText,
                           metaImage.language)

    def toApiVisualElement(visualElement: domain.VisualElement): api.VisualElement =
      api.VisualElement(visualElement.visualElement, visualElement.language)

    def toDomainConcept(concept: api.NewConcept, userInfo: UserInfo): Try[domain.Concept] = {
      Success(
        domain.Concept(
          id = None,
          revision = None,
          title = Seq(domain.ConceptTitle(concept.title, concept.language)),
          content = concept.content
            .map(content => Seq(domain.ConceptContent(content, concept.language)))
            .getOrElse(Seq.empty),
          copyright = concept.copyright.map(toDomainCopyright),
          source = concept.source,
          created = clock.now(),
          updated = clock.now(),
          updatedBy = Seq(userInfo.id),
          metaImage = concept.metaImage.map(m => domain.ConceptMetaImage(m.id, m.alt, concept.language)).toSeq,
          tags = concept.tags.map(t => toDomainTags(t, concept.language)).getOrElse(Seq.empty),
          subjectIds = concept.subjectIds.getOrElse(Seq.empty).toSet,
          articleIds = concept.articleIds.getOrElse(Seq.empty),
          status = Status.default,
          visualElement =
            concept.visualElement.filterNot(_.isEmpty).map(ve => domain.VisualElement(ve, concept.language)).toSeq
        ))
    }

    private def removeUnknownEmbedTagAttributes(html: String): String = {
      val document = HtmlTagRules.stringToJsoupDocument(html)
      document
        .select("embed")
        .asScala
        .map(el => {
          ResourceType
            .valueOf(el.attr(TagAttributes.DataResource.toString))
            .map(EmbedTagRules.attributesForResourceType)
            .map(knownAttributes => HtmlTagRules.removeIllegalAttributes(el, knownAttributes.all.map(_.toString)))
        })

      HtmlTagRules.jsoupDocumentToString(document)
    }

    private def toDomainVisualElement(visualElement: String, language: String): domain.VisualElement = {
      domain.VisualElement(
        visualElement = removeUnknownEmbedTagAttributes(visualElement),
        language = language
      )
    }

    private def toDomainTags(tags: Seq[String], language: String): Seq[domain.ConceptTags] =
      if (tags.isEmpty) Seq.empty else Seq(domain.ConceptTags(tags, language))

    def toDomainConcept(toMergeInto: domain.Concept,
                        updateConcept: api.UpdatedConcept,
                        userInfo: UserInfo): domain.Concept = {
      val domainTitle = updateConcept.title
        .map(t => domain.ConceptTitle(t, updateConcept.language))
        .toSeq
      val domainContent = updateConcept.content
        .map(c => domain.ConceptContent(c, updateConcept.language))
        .toSeq

      val domainTags = updateConcept.tags.map(t => domain.ConceptTags(t, updateConcept.language)).toSeq

      val domainVisualElement =
        updateConcept.visualElement.map(ve => toDomainVisualElement(ve, updateConcept.language)).toSeq

      val newMetaImage = updateConcept.metaImage match {
        case Left(_) => toMergeInto.metaImage.filterNot(_.language == updateConcept.language)
        case Right(meta) =>
          val domainMetaImage = meta
            .map(m => domain.ConceptMetaImage(m.id, m.alt, updateConcept.language))
            .toSeq
          mergeLanguageFields(toMergeInto.metaImage, domainMetaImage)
      }

      val updatedBy = {
        val userId = userInfo.id
        if (!toMergeInto.updatedBy.contains(userId)) toMergeInto.updatedBy :+ userId
        else toMergeInto.updatedBy
      }

      toMergeInto.copy(
        title = mergeLanguageFields(toMergeInto.title, domainTitle),
        content = mergeLanguageFields(toMergeInto.content, domainContent),
        copyright = updateConcept.copyright
          .map(toDomainCopyright)
          .orElse(toMergeInto.copyright),
        source = updateConcept.source,
        created = toMergeInto.created,
        updated = clock.now(),
        updatedBy = updatedBy,
        metaImage = newMetaImage,
        tags = mergeLanguageFields(toMergeInto.tags, domainTags),
        subjectIds = updateConcept.subjectIds.map(_.toSet).getOrElse(toMergeInto.subjectIds),
        articleIds = updateConcept.articleIds.map(_.toSeq).getOrElse(toMergeInto.articleIds),
        visualElement = mergeLanguageFields(toMergeInto.visualElement, domainVisualElement)
      )
    }

    def updateStatus(status: ConceptStatus.Value, concept: domain.Concept, user: UserInfo): IO[Try[domain.Concept]] =
      StateTransitionRules.doTransition(concept, status, user)

    def toDomainConcept(id: Long, concept: api.UpdatedConcept, userInfo: UserInfo): domain.Concept = {
      val lang = concept.language

      val newMetaImage = concept.metaImage match {
        case Right(meta) => meta.map(m => domain.ConceptMetaImage(m.id, m.alt, lang)).toSeq
        case Left(_)     => Seq.empty
      }

      domain.Concept(
        id = Some(id),
        revision = None,
        title = concept.title.map(t => domain.ConceptTitle(t, lang)).toSeq,
        content = concept.content.map(c => domain.ConceptContent(c, lang)).toSeq,
        copyright = concept.copyright.map(toDomainCopyright),
        source = concept.source,
        created = clock.now(),
        updated = clock.now(),
        updatedBy = Seq(userInfo.id),
        metaImage = newMetaImage,
        tags = concept.tags.map(t => toDomainTags(t, concept.language)).getOrElse(Seq.empty),
        subjectIds = concept.subjectIds.getOrElse(Seq.empty).toSet,
        articleIds = concept.articleIds.getOrElse(Seq.empty),
        status = Status.default,
        visualElement = concept.visualElement.map(ve => domain.VisualElement(ve, lang)).toSeq
      )
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(
        copyright.license.map(_.license),
        copyright.origin,
        copyright.creators.map(toDomainAuthor),
        copyright.processors.map(toDomainAuthor),
        copyright.rightsholders.map(toDomainAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toDomainAuthor(author: api.Author): domain.Author =
      domain.Author(author.`type`, author.name)

    private[service] def mergeLanguageFields[A <: LanguageField](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.isEmpty)
    }

    def toApiConceptTags(tags: Seq[String],
                         tagsCount: Int,
                         pageSize: Int,
                         offset: Int,
                         language: String): api.TagsSearchResult = {
      api.TagsSearchResult(tagsCount, offset, pageSize, language, tags)
    }

    def stateTransitionsToApi(user: UserInfo): Map[String, Seq[String]] = {
      StateTransitionRules.StateTransitions.groupBy(_.from).map {
        case (from, to) =>
          from.toString -> to
            .filter(t => user.hasRoles(t.requiredRoles))
            .map(_.to.toString)
            .toSeq
      }
    }

  }

}

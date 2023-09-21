/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import cats.implicits._
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.{Path, Url}
import no.ndla.common.model.domain.{Responsible, Tag, Title}
import no.ndla.common.Clock
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.{domain => commonDomain, api => commonApi}
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api.NotFoundException
import no.ndla.conceptapi.model.domain.{Concept, ConceptStatus, ConceptType, Status, WordClass}
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.repository.DraftConceptRepository
import no.ndla.language.Language.{AllLanguages, UnknownLanguage, findByLanguageOrBestEffort, mergeLanguageFields}
import no.ndla.mapping.License.getLicense
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.validation.{EmbedTagRules, HtmlTagRules, ResourceType, TagAttributes}
import org.jsoup.nodes.Element

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with DraftConceptRepository with StateTransitionRules with Props =>
  val converterService: ConverterService

  class ConverterService extends StrictLogging {
    import props.externalApiUrls

    def toApiConcept(concept: domain.Concept, language: String, fallback: Boolean): Try[api.Concept] = {
      val isLanguageNeutral =
        concept.supportedLanguages.contains(UnknownLanguage.toString) && concept.supportedLanguages.size == 1
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

        val responsible = concept.responsible.map(toApiConceptResponsible)

        Success(
          api.Concept(
            id = concept.id.get,
            revision = concept.revision.getOrElse(-1),
            title = title,
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
            visualElement = visualElement,
            responsible = responsible,
            conceptType = concept.conceptType.toString,
            glossData = toApiGlossData(concept.glossData)
          )
        )
      } else {
        Failure(
          NotFoundException(
            s"The concept with id ${concept.id.getOrElse(-1)} and language '$language' was not found.",
            concept.supportedLanguages.toSeq
          )
        )
      }
    }

    def toApiGlossData(domainGlossData: Option[domain.GlossData]): Option[api.GlossData] = {
      domainGlossData.map(glossData =>
        api.GlossData(
          gloss = glossData.gloss,
          wordClass = glossData.wordClass.toString,
          examples = glossData.examples.map(ge =>
            ge.map(g => api.GlossExample(example = g.example, language = g.language, transcriptions = g.transcriptions))
          ),
          originalLanguage = glossData.originalLanguage,
          transcriptions = glossData.transcriptions
        )
      )
    }

    def toApiStatus(status: domain.Status) = {
      api.Status(
        current = status.current.toString,
        other = status.other.map(_.toString).toSeq
      )
    }

    def toApiTags(tags: Tag) = {
      api.ConceptTags(
        tags.tags,
        tags.language
      )
    }

    def toApiCopyright(copyright: commonDomain.draft.DraftCopyright): commonApi.DraftCopyright = {
      commonApi.DraftCopyright(
        copyright.license.flatMap(toMaybeApiLicense),
        copyright.origin,
        copyright.creators.map(_.toApi),
        copyright.processors.map(_.toApi),
        copyright.rightsholders.map(_.toApi),
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toMaybeApiLicense(shortLicense: String): Option[commonApi.License] = {
      getLicense(shortLicense)
        .map(l => commonApi.License(l.license.toString, Option(l.description), l.url))
    }

    def toApiLicense(maybeShortLicense: Option[String]): commonApi.License =
      maybeShortLicense.flatMap(toMaybeApiLicense).getOrElse(commonApi.License("unknown", None, None))

    def toApiLicense(shortLicense: String): commonApi.License =
      toMaybeApiLicense(shortLicense).getOrElse(commonApi.License("unknown", None, None))

    def toApiConceptTitle(title: Title): api.ConceptTitle =
      api.ConceptTitle(title.title, title.language)

    def toApiConceptContent(title: domain.ConceptContent): api.ConceptContent =
      api.ConceptContent(title.content, title.language)

    def toApiMetaImage(metaImage: domain.ConceptMetaImage): api.ConceptMetaImage =
      api.ConceptMetaImage(
        s"${externalApiUrls("raw-image")}/${metaImage.imageId}",
        metaImage.altText,
        metaImage.language
      )

    def toApiVisualElement(visualElement: domain.VisualElement): api.VisualElement =
      api.VisualElement(converterService.addUrlOnElement(visualElement.visualElement), visualElement.language)

    def toApiConceptResponsible(responsible: Responsible): api.ConceptResponsible =
      api.ConceptResponsible(responsibleId = responsible.responsibleId, lastUpdated = responsible.lastUpdated)

    def toDomainGlossData(apiGlossData: Option[api.GlossData]): Try[Option[domain.GlossData]] = {
      apiGlossData
        .map(glossData =>
          WordClass.valueOfOrError(glossData.wordClass) match {
            case Failure(ex) => Failure(ex)
            case Success(wordClass) =>
              Success(
                domain.GlossData(
                  gloss = glossData.gloss,
                  wordClass = wordClass,
                  examples = glossData.examples.map(gl =>
                    gl.map(g =>
                      domain.GlossExample(language = g.language, example = g.example, transcriptions = g.transcriptions)
                    )
                  ),
                  originalLanguage = glossData.originalLanguage,
                  transcriptions = glossData.transcriptions
                )
              )
          }
        )
        .sequence
    }

    def toDomainConcept(concept: api.NewConcept, userInfo: TokenUser): Try[domain.Concept] = {
      val conceptType = ConceptType.valueOfOrError(concept.conceptType).getOrElse(ConceptType.CONCEPT)
      for {
        glossData <- toDomainGlossData(concept.glossData)
      } yield domain.Concept(
        id = None,
        revision = None,
        title = Seq(Title(concept.title, concept.language)),
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
          concept.visualElement.filterNot(_.isEmpty).map(ve => domain.VisualElement(ve, concept.language)).toSeq,
        responsible = concept.responsibleId.map(responsibleId => Responsible(responsibleId, clock.now())),
        conceptType = conceptType,
        glossData = glossData
      )
    }

    private def removeUnknownEmbedTagAttributes(html: String): String = {
      val document = HtmlTagRules.stringToJsoupDocument(html)
      document
        .select(EmbedTagName)
        .asScala
        .foreach(el => {
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

    private def toDomainTags(tags: Seq[String], language: String): Seq[Tag] =
      if (tags.isEmpty) Seq.empty else Seq(Tag(tags, language))

    def toDomainConcept(
        toMergeInto: domain.Concept,
        updateConcept: api.UpdatedConcept,
        userInfo: TokenUser
    ): Try[domain.Concept] = {
      val domainTitle = updateConcept.title
        .map(t => Title(t, updateConcept.language))
        .toSeq
      val domainContent = updateConcept.content
        .map(c => domain.ConceptContent(c, updateConcept.language))
        .toSeq

      val domainTags = updateConcept.tags.map(t => Tag(t, updateConcept.language)).toSeq

      val domainVisualElement =
        updateConcept.visualElement.map(ve => toDomainVisualElement(ve, updateConcept.language)).toSeq

      val updatedMetaImage = updateConcept.metaImage match {
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

      val responsible = (updateConcept.responsibleId, toMergeInto.responsible) match {
        case (Left(_), _)                       => None
        case (Right(Some(responsibleId)), None) => Some(Responsible(responsibleId, clock.now()))
        case (Right(Some(responsibleId)), Some(existing)) if existing.responsibleId != responsibleId =>
          Some(Responsible(responsibleId, clock.now()))
        case (Right(_), existing) => existing
      }

      toDomainGlossData(updateConcept.glossData).map(glossData =>
        domain.Concept(
          id = toMergeInto.id,
          revision = toMergeInto.revision,
          title = mergeLanguageFields(toMergeInto.title, domainTitle),
          content = mergeLanguageFields(toMergeInto.content, domainContent),
          copyright = updateConcept.copyright.map(toDomainCopyright).orElse(toMergeInto.copyright),
          source = updateConcept.source,
          created = toMergeInto.created,
          updated = clock.now(),
          updatedBy = updatedBy,
          metaImage = updatedMetaImage,
          tags = mergeLanguageFields(toMergeInto.tags, domainTags),
          subjectIds = updateConcept.subjectIds.map(_.toSet).getOrElse(toMergeInto.subjectIds),
          articleIds = updateConcept.articleIds.map(_.toSeq).getOrElse(toMergeInto.articleIds),
          status = toMergeInto.status,
          visualElement = mergeLanguageFields(toMergeInto.visualElement, domainVisualElement),
          responsible = responsible,
          conceptType = ConceptType.valueOf(updateConcept.conceptType).getOrElse(toMergeInto.conceptType),
          glossData = glossData
        )
      )
    }

    def updateStatus(status: ConceptStatus.Value, concept: domain.Concept, user: TokenUser): IO[Try[domain.Concept]] =
      StateTransitionRules.doTransition(concept, status, user)

    def toDomainConcept(id: Long, concept: api.UpdatedConcept, userInfo: TokenUser): domain.Concept = {
      val lang = concept.language

      val newMetaImage = concept.metaImage match {
        case Right(meta) => meta.map(m => domain.ConceptMetaImage(m.id, m.alt, lang)).toSeq
        case Left(_)     => Seq.empty
      }

      val responsible = concept.responsibleId match {
        case Left(_)                    => None
        case Right(Some(responsibleId)) => Some(Responsible(responsibleId, clock.now()))
        case Right(_)                   => None
      }

      // format: off
      val glossData = concept.glossData.map(gloss =>
        domain.GlossData(
          gloss = gloss.gloss,
          wordClass = WordClass.valueOf(gloss.wordClass).getOrElse(WordClass.NOUN), // Default to NOUN, this is NullDocumentConcept case, so we have to improvise
          examples = gloss.examples.map(ge =>
            ge.map(g => domain.GlossExample(language = g.language, example = g.example, transcriptions = g.transcriptions))),
          originalLanguage = gloss.originalLanguage,
          transcriptions = gloss.transcriptions
        )
      )
      // format: on

      domain.Concept(
        id = Some(id),
        revision = None,
        title = concept.title.map(t => Title(t, lang)).toSeq,
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
        visualElement = concept.visualElement.map(ve => domain.VisualElement(ve, lang)).toSeq,
        responsible = responsible,
        conceptType = ConceptType.valueOf(concept.conceptType).getOrElse(ConceptType.CONCEPT),
        glossData = glossData
      )
    }

    def toDomainCopyright(copyright: commonApi.DraftCopyright): commonDomain.draft.DraftCopyright = {
      commonDomain.draft.DraftCopyright(
        copyright.license.map(_.license),
        copyright.origin,
        copyright.creators.map(_.toDomain),
        copyright.processors.map(_.toDomain),
        copyright.rightsholders.map(_.toDomain),
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toApiConceptTags(
        tags: Seq[String],
        tagsCount: Int,
        pageSize: Int,
        offset: Int,
        language: String
    ): api.TagsSearchResult = {
      api.TagsSearchResult(tagsCount, offset, pageSize, language, tags)
    }

    def stateTransitionsToApi(user: TokenUser): Map[String, Seq[String]] = {
      StateTransitionRules.StateTransitions.groupBy(_.from).map { case (from, to) =>
        from.toString -> to
          .filter(t => user.hasPermissions(t.requiredPermissions))
          .map(_.to.toString)
          .toSeq
      }
    }

    def addUrlOnVisualElement(concept: Concept): Concept = {
      val visualElementWithUrls =
        concept.visualElement.map(visual => visual.copy(visualElement = addUrlOnElement(visual.visualElement)))
      concept.copy(visualElement = visualElementWithUrls)
    }

    private[service] def addUrlOnElement(content: String): String = {
      val doc = stringToJsoupDocument(content)

      val embedTags = doc.select(EmbedTagName).asScala.toList
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
          val x         = props
          val baseUrl   = Url.parse(x.externalApiUrls(resourceType))
          val pathParts = Path.parse(path).parts

          embedTag.attr(
            s"${TagAttributes.DataUrl}",
            baseUrl.addPathParts(pathParts).toString
          ): Unit
        case _ =>
      }
    }

  }

}

/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.{Path, Url}
import no.ndla.common.model.domain.{Responsible, Tag, Title, concept}
import no.ndla.common.model.domain.concept.{
  ConceptContent,
  ConceptEditorNote,
  ConceptMetaImage,
  ConceptStatus,
  ConceptType,
  GlossData,
  GlossExample,
  Status,
  VisualElement,
  WordClass,
  Concept as DomainConcept
}
import no.ndla.common.{Clock, model}
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.api.{Delete, Missing, UpdateWith}
import no.ndla.common.model.{api as commonApi, domain as commonDomain}
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api.{ConceptTags, NotFoundException}
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.repository.DraftConceptRepository
import no.ndla.language.Language.{AllLanguages, UnknownLanguage, findByLanguageOrBestEffort, mergeLanguageFields}
import no.ndla.mapping.License.getLicense
import no.ndla.network.tapir.auth.Permission.CONCEPT_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.validation.{EmbedTagRules, HtmlTagRules, ResourceType, TagAttribute}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with DraftConceptRepository with StateTransitionRules with Props =>
  val converterService: ConverterService

  class ConverterService extends StrictLogging {
    import props.externalApiUrls

    def toApiConcept(
        concept: DomainConcept,
        language: String,
        fallback: Boolean,
        user: Option[TokenUser]
    ): Try[api.Concept] = {
      val isLanguageNeutral =
        concept.supportedLanguages.contains(UnknownLanguage.toString) && concept.supportedLanguages.size == 1
      if (concept.supportedLanguages.contains(language) || fallback || isLanguageNeutral || language == AllLanguages) {
        val title = findByLanguageOrBestEffort(concept.title, language)
          .map(toApiConceptTitle)
          .getOrElse(api.ConceptTitle("", UnknownLanguage.toString))
        val content = findByLanguageOrBestEffort(concept.content, language)
          .map(toApiConceptContent)
          .getOrElse(api.ConceptContent("", "", UnknownLanguage.toString))
        val metaImage = findByLanguageOrBestEffort(concept.metaImage, language)
          .map(toApiMetaImage)
          .getOrElse(api.ConceptMetaImage("", "", UnknownLanguage.toString))

        val tags = findByLanguageOrBestEffort(concept.tags, language).map(toApiTags)

        val visualElement = findByLanguageOrBestEffort(concept.visualElement, language).map(toApiVisualElement)

        val responsible = concept.responsible.map(toApiConceptResponsible)
        val status      = toApiStatus(concept.status)
        val editorNotes = Option.when(user.hasPermission(CONCEPT_API_WRITE))(concept.editorNotes.map(toApiEditorNote))

        Success(
          api.Concept(
            id = concept.id.get,
            revision = concept.revision.getOrElse(-1),
            title = title,
            content = Some(content),
            copyright = concept.copyright.map(toApiCopyright),
            source = concept.copyright.flatMap(_.origin),
            metaImage = Some(metaImage),
            tags = tags,
            subjectIds = if (concept.subjectIds.isEmpty) None else Some(concept.subjectIds),
            created = concept.created,
            updated = concept.updated,
            updatedBy = if (concept.updatedBy.isEmpty) None else Some(concept.updatedBy),
            supportedLanguages = concept.supportedLanguages,
            articleIds = concept.articleIds,
            status = status,
            visualElement = visualElement,
            responsible = responsible,
            conceptType = concept.conceptType.entryName,
            glossData = toApiGlossData(concept.glossData),
            editorNotes = editorNotes
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

    def toApiGlossData(domainGlossData: Option[GlossData]): Option[api.GlossData] = {
      domainGlossData.map(glossData =>
        api.GlossData(
          gloss = glossData.gloss,
          wordClass = glossData.wordClass.entryName,
          examples = glossData.examples.map(ge =>
            ge.map(g =>
              api.GlossExample(
                example = g.example,
                language = g.language,
                transcriptions = g.transcriptions
              )
            )
          ),
          originalLanguage = glossData.originalLanguage,
          transcriptions = glossData.transcriptions
        )
      )
    }

    def toApiStatus(status: Status): api.Status = {
      api.Status(
        current = status.current.toString,
        other = status.other.map(_.toString).toSeq
      )
    }
    private def toApiEditorNote(editorNote: ConceptEditorNote) = {
      api.EditorNote(
        note = editorNote.note,
        updatedBy = editorNote.user,
        status = toApiStatus(editorNote.status),
        timestamp = editorNote.timestamp
      )
    }

    def toApiTags(tags: Tag): ConceptTags = {
      api.ConceptTags(
        tags.tags,
        tags.language
      )
    }

    private def toApiCopyright(copyright: commonDomain.draft.DraftCopyright): commonApi.DraftCopyright = {
      commonApi.DraftCopyright(
        copyright.license.flatMap(toMaybeApiLicense),
        copyright.origin,
        copyright.creators.map(_.toApi),
        copyright.processors.map(_.toApi),
        copyright.rightsholders.map(_.toApi),
        copyright.validFrom,
        copyright.validTo,
        copyright.processed
      )
    }

    private def toMaybeApiLicense(shortLicense: String): Option[commonApi.License] = {
      getLicense(shortLicense)
        .map(l => commonApi.License(l.license.toString, Option(l.description), l.url))
    }

    def toApiLicense(maybeShortLicense: Option[String]): commonApi.License =
      maybeShortLicense.flatMap(toMaybeApiLicense).getOrElse(commonApi.License("unknown", None, None))

    def toApiLicense(shortLicense: String): commonApi.License =
      toMaybeApiLicense(shortLicense).getOrElse(commonApi.License("unknown", None, None))

    def toApiConceptTitle(title: Title): api.ConceptTitle =
      api.ConceptTitle(title.title, title.language)

    def toApiConceptContent(content: ConceptContent): api.ConceptContent =
      api.ConceptContent(Jsoup.parseBodyFragment(content.content).body().text(), content.content, content.language)

    def toApiMetaImage(metaImage: ConceptMetaImage): api.ConceptMetaImage =
      api.ConceptMetaImage(
        s"${externalApiUrls("raw-image")}/${metaImage.imageId}",
        metaImage.altText,
        metaImage.language
      )

    def toApiVisualElement(visualElement: VisualElement): api.VisualElement =
      api.VisualElement(converterService.addUrlOnElement(visualElement.visualElement), visualElement.language)

    private def toApiConceptResponsible(responsible: Responsible): api.ConceptResponsible =
      api.ConceptResponsible(responsibleId = responsible.responsibleId, lastUpdated = responsible.lastUpdated)

    def toDomainGlossData(apiGlossData: Option[api.GlossData]): Try[Option[GlossData]] = {
      apiGlossData
        .map(glossData =>
          WordClass.valueOfOrError(glossData.wordClass) match {
            case Failure(ex) => Failure(ex)
            case Success(wordClass) =>
              Success(
                concept.GlossData(
                  gloss = glossData.gloss,
                  wordClass = wordClass,
                  examples = glossData.examples.map(gl =>
                    gl.map(g =>
                      GlossExample(language = g.language, example = g.example, transcriptions = g.transcriptions)
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

    def toDomainConcept(concept: api.NewConcept, userInfo: TokenUser): Try[DomainConcept] = {
      val conceptType = ConceptType.valueOfOrError(concept.conceptType).getOrElse(ConceptType.CONCEPT)
      val content = concept.content
        .map(content => Seq(model.domain.concept.ConceptContent(content, concept.language)))
        .getOrElse(Seq.empty)
      val visualElement = concept.visualElement
        .filterNot(_.isEmpty)
        .map(ve => toDomainVisualElement(ve, concept.language))
        .toSeq
      val now = clock.now()

      for {
        glossData <- toDomainGlossData(concept.glossData)
      } yield DomainConcept(
        id = None,
        revision = None,
        title = Seq(Title(concept.title, concept.language)),
        content = content,
        copyright = concept.copyright.map(toDomainCopyright),
        created = now,
        updated = now,
        updatedBy = Seq(userInfo.id),
        metaImage =
          concept.metaImage.map(m => model.domain.concept.ConceptMetaImage(m.id, m.alt, concept.language)).toSeq,
        tags = concept.tags.map(t => toDomainTags(t, concept.language)).getOrElse(Seq.empty),
        subjectIds = concept.subjectIds.getOrElse(Seq.empty).toSet,
        articleIds = concept.articleIds.getOrElse(Seq.empty),
        status = Status.default,
        visualElement = visualElement,
        responsible = concept.responsibleId.map(responsibleId => Responsible(responsibleId, clock.now())),
        conceptType = conceptType,
        glossData = glossData,
        editorNotes = Seq(ConceptEditorNote(s"Created $conceptType", userInfo.id, Status.default, now))
      )
    }

    private def removeUnknownEmbedTagAttribute(html: String): String = {
      val document = HtmlTagRules.stringToJsoupDocument(html)
      document
        .select(EmbedTagName)
        .asScala
        .foreach(el => {
          ResourceType
            .withNameOption(el.attr(TagAttribute.DataResource.toString))
            .map(EmbedTagRules.attributesForResourceType)
            .map(knownAttributes => HtmlTagRules.removeIllegalAttributes(el, knownAttributes.all.map(_.toString)))
        })

      HtmlTagRules.jsoupDocumentToString(document)
    }

    private def toDomainVisualElement(visualElement: String, language: String): VisualElement = {
      concept.VisualElement(
        visualElement = removeUnknownEmbedTagAttribute(visualElement),
        language = language
      )
    }

    private def toDomainTags(tags: Seq[String], language: String): Seq[Tag] =
      if (tags.isEmpty) Seq.empty else Seq(Tag(tags, language))

    def toDomainConcept(
        toMergeInto: DomainConcept,
        updateConcept: api.UpdatedConcept,
        userInfo: TokenUser
    ): Try[DomainConcept] = {
      val domainTitle = updateConcept.title
        .map(t => Title(t, updateConcept.language))
        .toSeq
      val domainContent = updateConcept.content
        .map(c => concept.ConceptContent(c, updateConcept.language))
        .toSeq

      val domainTags = updateConcept.tags.map(t => Tag(t, updateConcept.language)).toSeq

      val domainVisualElement =
        updateConcept.visualElement.map(ve => toDomainVisualElement(ve, updateConcept.language)).toSeq

      val updatedMetaImage = updateConcept.metaImage match {
        case Delete => toMergeInto.metaImage.filterNot(_.language == updateConcept.language)
        case UpdateWith(m) =>
          val domainMetaImage = concept.ConceptMetaImage(m.id, m.alt, updateConcept.language)
          mergeLanguageFields(toMergeInto.metaImage, Seq(domainMetaImage))
        case Missing => toMergeInto.metaImage
      }

      val updatedBy = {
        val userId = userInfo.id
        if (!toMergeInto.updatedBy.contains(userId)) toMergeInto.updatedBy :+ userId
        else toMergeInto.updatedBy
      }

      val responsible = (updateConcept.responsibleId, toMergeInto.responsible) match {
        case (Delete, _)                       => None
        case (UpdateWith(responsibleId), None) => Some(Responsible(responsibleId, clock.now()))
        case (UpdateWith(responsibleId), Some(existing)) if existing.responsibleId != responsibleId =>
          Some(Responsible(responsibleId, clock.now()))
        case (_, existing) => existing
      }

      toDomainGlossData(updateConcept.glossData).map(glossData =>
        DomainConcept(
          id = toMergeInto.id,
          revision = toMergeInto.revision,
          title = mergeLanguageFields(toMergeInto.title, domainTitle),
          content = mergeLanguageFields(toMergeInto.content, domainContent),
          copyright = updateConcept.copyright.map(toDomainCopyright).orElse(toMergeInto.copyright),
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
          glossData = glossData,
          editorNotes = toMergeInto.editorNotes
        )
      )
    }

    def updateStatus(status: ConceptStatus, concept: DomainConcept, user: TokenUser): Try[DomainConcept] =
      StateTransitionRules.doTransition(concept, status, user)

    def toDomainConcept(id: Long, concept: api.UpdatedConcept, userInfo: TokenUser): DomainConcept = {
      val lang = concept.language

      val newMetaImage = concept.metaImage match {
        case UpdateWith(m) => Seq(model.domain.concept.ConceptMetaImage(m.id, m.alt, lang))
        case _             => Seq.empty
      }

      val responsible = concept.responsibleId match {
        case UpdateWith(responsibleId) => Some(Responsible(responsibleId, clock.now()))
        case _                         => None
      }

      // format: off
      val glossData = concept.glossData.map(gloss =>
        model.domain.concept.GlossData(
          gloss = gloss.gloss,
          wordClass = WordClass.valueOf(gloss.wordClass).getOrElse(WordClass.NOUN), // Default to NOUN, this is NullDocumentConcept case, so we have to improvise
          examples = gloss.examples.map(ge =>
            ge.map(g => model.domain.concept.GlossExample(language = g.language, example = g.example, transcriptions = g.transcriptions))),
          originalLanguage = gloss.originalLanguage,
          transcriptions = gloss.transcriptions
        )
      )
      // format: on

      val conceptType = ConceptType.valueOf(concept.conceptType).getOrElse(ConceptType.CONCEPT)

      DomainConcept(
        id = Some(id),
        revision = None,
        title = concept.title.map(t => Title(t, lang)).toSeq,
        content = concept.content.map(c => model.domain.concept.ConceptContent(c, lang)).toSeq,
        copyright = concept.copyright.map(toDomainCopyright),
        created = clock.now(),
        updated = clock.now(),
        updatedBy = Seq(userInfo.id),
        metaImage = newMetaImage,
        tags = concept.tags.map(t => toDomainTags(t, concept.language)).getOrElse(Seq.empty),
        subjectIds = concept.subjectIds.getOrElse(Seq.empty).toSet,
        articleIds = concept.articleIds.getOrElse(Seq.empty),
        status = Status.default,
        visualElement = concept.visualElement.map(ve => toDomainVisualElement(ve, lang)).toSeq,
        responsible = responsible,
        conceptType = conceptType,
        glossData = glossData,
        editorNotes = Seq(ConceptEditorNote(s"Created $conceptType", userInfo.id, Status.default, clock.now()))
      )
    }

    private def toDomainCopyright(copyright: commonApi.DraftCopyright): commonDomain.draft.DraftCopyright =
      commonDomain.draft.DraftCopyright(
        copyright.license.map(_.license),
        copyright.origin,
        copyright.creators.map(_.toDomain),
        copyright.processors.map(_.toDomain),
        copyright.rightsholders.map(_.toDomain),
        copyright.validFrom,
        copyright.validTo,
        copyright.processed
      )

    def toApiConceptTags(
        tags: Seq[String],
        tagsCount: Int,
        pageSize: Int,
        offset: Int,
        language: String
    ): api.TagsSearchResult = {
      api.TagsSearchResult(tagsCount, offset, pageSize, language, tags)
    }

    def stateTransitionsToApi(user: TokenUser): Map[String, List[String]] =
      StateTransitionRules.StateTransitions.groupBy(_.from).map { case (from, to) =>
        from.toString -> to
          .filter(t => user.hasPermissions(t.requiredPermissions))
          .map(_.to.toString)
          .toList
      }

    def addUrlOnVisualElement(concept: DomainConcept): DomainConcept = {
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
      val typeAndPathOption = embedTag.attr(TagAttribute.DataResource.toString) match {
        case resourceType
            if resourceType == ResourceType.H5P.toString
              && embedTag.hasAttr(TagAttribute.DataPath.toString) =>
          val path = embedTag.attr(TagAttribute.DataPath.toString)
          Some((resourceType, path))

        case resourceType if embedTag.hasAttr(TagAttribute.DataResource_Id.toString) =>
          val id = embedTag.attr(TagAttribute.DataResource_Id.toString)
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
            s"${TagAttribute.DataUrl}",
            baseUrl.addPathParts(pathParts).toString
          ): Unit
        case _ =>
      }
    }

  }

}

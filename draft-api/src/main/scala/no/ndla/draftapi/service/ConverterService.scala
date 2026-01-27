/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.converter.CommonConverter
import no.ndla.common.errors.ValidationException
import no.ndla.common.implicits.*
import no.ndla.common.model.api.{Delete, DisclaimerDTO, DraftCopyrightDTO, Missing, ResponsibleDTO, UpdateWith}
import no.ndla.common.model.domain.{ArticleContent, Priority, Responsible}
import no.ndla.common.model.domain.draft.DraftStatus.PLANNED
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.language.OptLanguageFields
import no.ndla.common.model.{RelatedContentLink, api as commonApi, domain as common}
import no.ndla.common.{Clock, model}
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.api.{NotFoundException, UpdatedArticleDTO}
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.database.DBUtility
import no.ndla.language.Language.{AllLanguages, UnknownLanguage, findByLanguageOrBestEffort, mergeLanguageFields}
import no.ndla.mapping.License.getLicense
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}
import common.getNextRevision
import no.ndla.common.util.TraitUtil

class ConverterService(using
    clock: Clock,
    draftRepository: DraftRepository,
    commonConverter: CommonConverter,
    writeService: => WriteService,
    props: DraftApiProperties,
    traitUtil: TraitUtil,
    dbUtility: DBUtility,
) extends StrictLogging {
  def toDomainArticle(newArticleId: Long, newArticle: api.NewArticleDTO, user: TokenUser): Try[Draft] = {
    val domainTitles  = Seq(common.Title(newArticle.title, newArticle.language))
    val domainContent = newArticle
      .content
      .map(content => common.ArticleContent(removeUnknownEmbedTagAttribute(content), newArticle.language))
      .toSeq
    val domainDisclaimer = OptLanguageFields.fromMaybeString(newArticle.disclaimer, newArticle.language)
    val status           = common.Status(PLANNED, Set.empty)

    val newAvailability = common.Availability.valueOf(newArticle.availability).getOrElse(common.Availability.everyone)
    val revisionMeta    = newArticle.revisionMeta match {
      case Some(revs) if revs.nonEmpty =>
        newArticle
          .revisionMeta
          .map(_.map(commonConverter.revisionMetaApiToDomain))
          .getOrElse(common.RevisionMeta.default)
      case _ => common.RevisionMeta.default
    }

    val responsible = newArticle
      .responsibleId
      .map(responsibleId => Responsible(responsibleId = responsibleId, lastUpdated = clock.now()))

    val priority  = newArticle.priority.getOrElse(Priority.Unspecified)
    val libraries = newArticle.requiredLibraries.getOrElse(Seq.empty)
    val content   = domainContent.filterNot(_.isEmpty)
    val now       = clock.now()
    val traits    = traitUtil.getArticleTraits(content)
    val comments  = newArticle
      .comments
      .map(comments => comments.map(commonConverter.newCommentApiToDomain))
      .getOrElse(List.empty)

    val visualElement = newArticle.visualElement.map(visual => toDomainVisualElement(visual, newArticle.language)).toSeq
    val introduction  = newArticle
      .introduction
      .map(intro => toDomainIntroduction(intro, newArticle.language))
      .filterNot(_.isEmpty)
      .toSeq
    val metaDescription = newArticle
      .metaDescription
      .map(meta => toDomainMetaDescription(meta, newArticle.language))
      .filterNot(_.isEmpty)
      .toSeq

    newNotes(newArticle.notes.getOrElse(Seq.empty), user, status).map(notes =>
      Draft(
        id = Some(newArticleId),
        revision = None,
        status,
        title = domainTitles,
        content = content,
        copyright = newArticle.copyright.map(toDomainCopyright),
        tags = toDomainTag(newArticle.tags, newArticle.language).toSeq,
        requiredLibraries = libraries.map(toDomainRequiredLibraries),
        visualElement = visualElement,
        introduction = introduction,
        metaDescription = metaDescription,
        metaImage =
          newArticle.metaImage.map(meta => toDomainMetaImage(meta, newArticle.language)).filterNot(_.isEmpty).toSeq,
        created = now,
        updated = now,
        updatedBy = user.id,
        published = newArticle.published.getOrElse(now),
        articleType = common.ArticleType.valueOfOrError(newArticle.articleType),
        notes = notes,
        previousVersionsNotes = Seq.empty,
        editorLabels = newArticle.editorLabels.getOrElse(Seq.empty),
        grepCodes = newArticle.grepCodes.getOrElse(Seq.empty),
        conceptIds = newArticle.conceptIds.getOrElse(Seq.empty),
        availability = newAvailability,
        relatedContent = toDomainRelatedContent(newArticle.relatedContent.getOrElse(Seq.empty)),
        revisionMeta = revisionMeta,
        responsible = responsible,
        slug = newArticle.slug,
        comments = comments,
        priority = priority,
        started = false,
        qualityEvaluation = qualityEvaluationToDomain(newArticle.qualityEvaluation),
        disclaimer = domainDisclaimer,
        traits = traits,
      )
    )
  }

  private[service] def qualityEvaluationToDomain(
      newQualityEvaluation: Option[api.QualityEvaluationDTO]
  ): Option[common.draft.QualityEvaluation] =
    newQualityEvaluation.map(qe => common.draft.QualityEvaluation(grade = qe.grade, note = qe.note))

  def withSortedLanguageFields(article: Draft): Draft = {
    article.copy(
      visualElement = article.visualElement.sorted,
      content = article.content.sorted,
      introduction = article.introduction.sorted,
      metaImage = article.metaImage.sorted,
      title = article.title.sorted,
    )
  }

  private[service] def newNotes(
      notes: Seq[String],
      user: TokenUser,
      status: common.Status,
  ): Try[Seq[common.EditorNote]] = {
    notes match {
      case Nil                  => Success(Seq.empty)
      case l if !l.contains("") => Success(l.map(common.EditorNote(_, user.id, status, clock.now())))
      case _                    => Failure(ValidationException("notes", "A note can not be an empty string"))
    }
  }

  private def toDomainRelatedContent(relatedContent: Seq[commonApi.RelatedContent]): Seq[common.RelatedContent] = {
    relatedContent.map {
      case Left(x)  => Left(RelatedContentLink(url = x.url, title = x.title))
      case Right(x) => Right(x)
    }

  }

  private def toDomainTitle(articleTitle: api.ArticleTitleDTO): common.Title =
    common.Title(articleTitle.title, articleTitle.language)

  private def toDomainContent(articleContent: api.ArticleContentDTO): common.ArticleContent = {
    common.ArticleContent(removeUnknownEmbedTagAttribute(articleContent.content), articleContent.language)
  }

  private def toDomainTag(tag: Option[Seq[String]], language: String): Option[common.Tag] = tag.flatMap {
    case list if list.nonEmpty => Some(common.Tag(list, language))
    case _                     => None
  }

  private def toDomainVisualElement(visual: String, language: String): common.VisualElement =
    common.VisualElement(removeUnknownEmbedTagAttribute(visual), language)

  private def toDomainIntroduction(intro: String, language: String): common.Introduction =
    common.Introduction(intro, language)

  private def toDomainMetaDescription(meta: String, language: String): common.Description =
    common.Description(meta, language)

  private def toDomainMetaImage(metaImage: api.NewArticleMetaImageDTO, language: String): common.ArticleMetaImage =
    common.ArticleMetaImage(metaImage.id, metaImage.alt, language)

  private def toDomainCopyright(copyright: DraftCopyrightDTO): common.draft.DraftCopyright = {
    common
      .draft
      .DraftCopyright(
        license = copyright.license.map(_.license),
        origin = copyright.origin,
        creators = copyright.creators.map(_.toDomain),
        processors = copyright.processors.map(_.toDomain),
        rightsholders = copyright.rightsholders.map(_.toDomain),
        validFrom = copyright.validFrom,
        validTo = copyright.validTo,
        processed = copyright.processed,
      )
  }

  def getEmbeddedConceptIds(article: Draft): Seq[Long] = {
    val htmlElements  = article.content.map(content => HtmlTagRules.stringToJsoupDocument(content.content))
    val conceptEmbeds = htmlElements.flatMap(elem => {
      val conceptSelector = s"$EmbedTagName[${TagAttribute.DataResource}=${ResourceType.Concept}]"
      elem.select(conceptSelector).asScala.toSeq
    })

    val conceptIds = conceptEmbeds.flatMap(embed => {
      Try(embed.attr(TagAttribute.DataContentId.toString).toLong) match {
        case Failure(ex) =>
          logger.error(s"Could not derive concept id from embed: '${embed.toString}'", ex)
          None
        case Success(id) => Some(id)
      }
    })
    conceptIds
  }

  def getEmbeddedH5PPaths(article: Draft): Seq[String] = {
    val getH5PEmbeds = (htmlElements: Seq[Element]) => {
      htmlElements.flatMap(elem => {
        val h5pSelector = s"$EmbedTagName[${TagAttribute.DataResource}=${ResourceType.H5P}]"
        elem.select(h5pSelector).asScala.toSeq
      })
    }

    val htmlElements   = article.content.map(content => HtmlTagRules.stringToJsoupDocument(content.content))
    val visualElements = article.visualElement.map(ve => HtmlTagRules.stringToJsoupDocument(ve.resource))

    val h5pEmbeds = getH5PEmbeds(htmlElements) ++ getH5PEmbeds(visualElements)

    h5pEmbeds.flatMap(embed => {
      Try(embed.attr(TagAttribute.DataPath.toString)) match {
        case Success(path) if path.isEmpty =>
          logger.error(s"Could not derive h5p path (empty string) from embed: '${embed.toString}'")
          None
        case Failure(ex) =>
          logger.error(s"Could not derive h5p path from embed: '${embed.toString}'", ex)
          None
        case Success(path) => Some(path)
      }
    })
  }

  private def toDomainRequiredLibraries(requiredLibs: api.RequiredLibraryDTO): common.RequiredLibrary = {
    common.RequiredLibrary(requiredLibs.mediaType, requiredLibs.name, requiredLibs.url)
  }

  private def getLinkToOldNdla(id: Long): Option[String] = dbUtility
    .readOnly { session =>
      draftRepository.getExternalIdsFromId(id)(using session)
    }
    .toOption
    .flatMap(_.map(createLinkToOldNdla).headOption)

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

  private def toApiResponsible(responsible: Responsible): ResponsibleDTO =
    ResponsibleDTO(responsibleId = responsible.responsibleId, lastUpdated = responsible.lastUpdated)

  def toApiArticle(article: Draft, language: String, fallback: Boolean = false): Try[api.ArticleDTO] = {
    val isLanguageNeutral = article.supportedLanguages.contains(UnknownLanguage.toString) && article
      .supportedLanguages
      .length == 1

    if (article.supportedLanguages.contains(language) || language == AllLanguages || isLanguageNeutral || fallback) {
      val metaDescription =
        findByLanguageOrBestEffort(article.metaDescription, language).map(toApiArticleMetaDescription)
      val tags           = findByLanguageOrBestEffort(article.tags, language).map(toApiArticleTag)
      val title          = findByLanguageOrBestEffort(article.title, language).map(toApiArticleTitle)
      val introduction   = findByLanguageOrBestEffort(article.introduction, language).map(toApiArticleIntroduction)
      val visualElement  = findByLanguageOrBestEffort(article.visualElement, language).map(toApiVisualElement)
      val articleContent = findByLanguageOrBestEffort(article.content, language).map(toApiArticleContent)
      val metaImage      = findByLanguageOrBestEffort(article.metaImage, language).map(toApiArticleMetaImage)
      val revisionMetas  = article.revisionMeta.map(commonConverter.revisionMetaDomainToApi)
      val responsible    = article.responsible.map(toApiResponsible)
      val disclaimer     = article.disclaimer.findByLanguageOrBestEffort(language).map(DisclaimerDTO.fromLanguageValue)

      Success(
        api.ArticleDTO(
          id = article.id.get,
          oldNdlaUrl = article.id.flatMap(getLinkToOldNdla),
          revision = article.revision.get,
          status = toApiStatus(article.status),
          title = title,
          content = articleContent,
          copyright = article.copyright.map(lic => toApiCopyright(lic, language)),
          tags = tags,
          requiredLibraries = article.requiredLibraries.map(toApiRequiredLibrary),
          visualElement = visualElement,
          introduction = introduction,
          metaDescription = metaDescription,
          metaImage = metaImage,
          created = article.created,
          updated = article.updated,
          updatedBy = article.updatedBy,
          published = article.published,
          articleType = article.articleType.entryName,
          supportedLanguages = article.supportedLanguages,
          notes = article.notes.map(toApiEditorNote),
          editorLabels = article.editorLabels,
          grepCodes = article.grepCodes,
          conceptIds = article.conceptIds,
          availability = article.availability.toString,
          relatedContent = article.relatedContent.map(toApiRelatedContent),
          revisions = revisionMetas,
          responsible = responsible,
          slug = article.slug,
          comments = article.comments.map(commonConverter.commentDomainToApi),
          priority = article.priority,
          started = article.started,
          qualityEvaluation = toApiQualityEvaluation(article.qualityEvaluation),
          disclaimer = disclaimer,
          traits = article.traits,
        )
      )
    } else {
      Failure(
        NotFoundException(
          s"The article with id ${article.id.get} and language $language was not found",
          article.supportedLanguages,
        )
      )
    }
  }

  def toApiUserData(userData: domain.UserData): api.UserDataDTO = {
    api.UserDataDTO(
      userId = userData.userId,
      savedSearches = userData.savedSearches,
      latestEditedArticles = userData.latestEditedArticles,
      latestEditedConcepts = userData.latestEditedConcepts,
      latestEditedLearningpaths = userData.latestEditedLearningpaths,
      favoriteSubjects = userData.favoriteSubjects,
    )
  }

  private def toApiEditorNote(note: common.EditorNote): api.EditorNoteDTO =
    api.EditorNoteDTO(note.note, note.user, toApiStatus(note.status), note.timestamp)

  private def toApiStatus(status: common.Status): api.StatusDTO =
    api.StatusDTO(status.current.toString, status.other.map(_.toString).toSeq)

  def toApiArticleTitle(title: common.Title): api.ArticleTitleDTO =
    api.ArticleTitleDTO(Jsoup.parseBodyFragment(title.title).body().text(), title.title, title.language)

  private def toApiArticleContent(content: common.ArticleContent): api.ArticleContentDTO =
    api.ArticleContentDTO(content.content, content.language)

  private def toApiArticleMetaImage(metaImage: common.ArticleMetaImage): api.ArticleMetaImageDTO = {
    api.ArticleMetaImageDTO(
      s"${props.externalApiUrls("raw-image")}/${metaImage.imageId}",
      metaImage.altText,
      metaImage.language,
    )
  }

  private def toApiCopyright(copyright: common.draft.DraftCopyright, language: String): DraftCopyrightDTO = {
    model
      .api
      .DraftCopyrightDTO(
        copyright.license.map(lic => toApiLicense(lic, language)),
        copyright.origin,
        copyright.creators.map(_.toApi),
        copyright.processors.map(_.toApi),
        copyright.rightsholders.map(_.toApi),
        copyright.validFrom,
        copyright.validTo,
        copyright.processed,
      )
  }

  def toApiLicense(shortLicense: String, language: String): commonApi.LicenseDTO = {
    getLicense(shortLicense)
      .map(l =>
        commonApi.LicenseDTO(
          l.license.toString,
          Option(l.description),
          findByLanguageOrBestEffort(l.url, language).map(_.url),
        )
      )
      .getOrElse(commonApi.LicenseDTO("unknown", None, None))
  }

  private def toApiRelatedContent(relatedContent: common.RelatedContent): commonApi.RelatedContent = {
    relatedContent match {
      case Left(x)  => Left(commonApi.RelatedContentLinkDTO(url = x.url, title = x.title))
      case Right(x) => Right(x)
    }
  }

  private def toApiQualityEvaluation(
      qualityEvaluation: Option[common.draft.QualityEvaluation]
  ): Option[api.QualityEvaluationDTO] = {
    qualityEvaluation.map(qe => api.QualityEvaluationDTO(grade = qe.grade, note = qe.note))
  }

  def toApiArticleTag(tag: common.Tag): api.ArticleTagDTO = api.ArticleTagDTO(tag.tags, tag.language)

  private def toApiRequiredLibrary(required: common.RequiredLibrary): api.RequiredLibraryDTO = {
    api.RequiredLibraryDTO(required.mediaType, required.name, required.url)
  }

  def toApiVisualElement(visual: common.VisualElement): api.VisualElementDTO =
    api.VisualElementDTO(visual.resource, visual.language)

  def toApiArticleIntroduction(intro: common.Introduction): api.ArticleIntroductionDTO = {
    api.ArticleIntroductionDTO(
      Jsoup.parseBodyFragment(intro.introduction).body().text(),
      intro.introduction,
      intro.language,
    )
  }

  private def toApiArticleMetaDescription(metaDescription: common.Description): api.ArticleMetaDescriptionDTO = {
    api.ArticleMetaDescriptionDTO(metaDescription.content, metaDescription.language)
  }

  private def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

  private def toArticleApiCopyright(copyright: common.draft.DraftCopyright): common.article.Copyright = {
    common
      .article
      .Copyright(
        copyright.license.getOrElse(""),
        copyright.origin,
        copyright.creators,
        copyright.processors,
        copyright.rightsholders,
        copyright.validFrom,
        copyright.validTo,
        copyright.processed,
      )
  }

  def deleteLanguage(article: Draft, language: String, userInfo: TokenUser): Try[Draft] = {
    val title               = article.title.filter(_.language != language)
    val content             = article.content.filter(_.language != language)
    val articleIntroduction = article.introduction.filter(_.language != language)
    val metaDescription     = article.metaDescription.filter(_.language != language)
    val tags                = article.tags.filter(_.language != language)
    val metaImage           = article.metaImage.filter(_.language != language)
    val visualElement       = article.visualElement.filter(_.language != language)
    val disclaimers         = article.disclaimer.dropLanguage(language)
    newNotes(Seq(s"Slettet språkvariant '$language'."), userInfo, article.status) match {
      case Failure(ex)             => Failure(ex)
      case Success(newEditorNotes) => Success(
          article.copy(
            title = title,
            content = content,
            introduction = articleIntroduction,
            metaDescription = metaDescription,
            tags = tags,
            metaImage = metaImage,
            visualElement = visualElement,
            notes = article.notes ++ newEditorNotes,
            disclaimer = disclaimers,
          )
        )
    }
  }

  def filterComments(content: Seq[ArticleContent]): Seq[ArticleContent] = {
    val contents = content.map(cont => {
      val document = Jsoup.parseBodyFragment(cont.content)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false).indentAmount(0)

      val commentEmbeds = document.select("[data-resource='comment']")
      commentEmbeds.unwrap()

      val newContentString = document.select("body").first().html()
      cont.copy(content = newContentString)
    })
    contents
  }

  def toArticleApiArticle(draft: Draft): Try[common.article.Article] = {
    draft.copyright match {
      case None            => Failure(ValidationException("copyright", "Copyright must be present when publishing an article"))
      case Some(copyright) => Success(
          common
            .article
            .Article(
              id = draft.id,
              revision = draft.revision,
              title = draft.title,
              content = filterComments(draft.content),
              copyright = toArticleApiCopyright(copyright),
              tags = draft.tags,
              requiredLibraries = draft.requiredLibraries,
              visualElement = draft.visualElement,
              introduction = draft.introduction,
              metaDescription = draft.metaDescription,
              metaImage = draft.metaImage,
              created = draft.created,
              updated = draft.updated,
              updatedBy = draft.updatedBy,
              published = draft.published,
              articleType = draft.articleType,
              grepCodes = draft.grepCodes,
              conceptIds = draft.conceptIds,
              availability = draft.availability,
              relatedContent = draft.relatedContent,
              revisionDate = draft.revisionMeta.getNextRevision.map(_.revisionDate),
              slug = draft.slug,
              disclaimer = draft.disclaimer,
              traits = draft.traits,
            )
        )
    }
  }

  private def languageFieldIsDefined(article: api.UpdatedArticleDTO): Boolean = {
    val metaImageExists = article.metaImage match {
      case UpdateWith(_) => true
      case _             => false
    }

    val langFields: Seq[Option[?]] = Seq(
      article.title,
      article.content,
      article.tags,
      article.introduction,
      article.metaDescription,
      article.visualElement,
      article.disclaimer,
    )

    langFields.foldRight(false)((curr, res) => res || curr.isDefined || metaImageExists)
  }

  private def getExistingPaths(content: String): Seq[String] = {
    val doc        = HtmlTagRules.stringToJsoupDocument(content)
    val fileEmbeds = doc.select(s"$EmbedTagName[${TagAttribute.DataResource}='${ResourceType.File}']").asScala.toSeq
    fileEmbeds.flatMap(e => Option(e.attr(TagAttribute.DataPath.toString)))
  }

  private def cloneFilesIfExists(existingContent: Seq[String], newContent: String): Try[String] = {
    val existingFiles = existingContent.flatMap(getExistingPaths)

    val doc        = HtmlTagRules.stringToJsoupDocument(newContent)
    val fileEmbeds = doc.select(s"$EmbedTagName[${TagAttribute.DataResource}='${ResourceType.File}']").asScala

    val embedsToCloneFile = fileEmbeds.filter(embed => {
      Option(embed.attr(TagAttribute.DataPath.toString)).exists(dataPath => existingFiles.contains(dataPath))
    })

    val cloned = embedsToCloneFile.toList.map(writeService.cloneEmbedAndUpdateElement).sequence

    cloned match {
      case Failure(ex) => Failure(ex)
      case Success(_)  => Success(HtmlTagRules.jsoupDocumentToString(doc))
    }
  }

  private def cloneFilesForOtherLanguages(
      content: Option[String],
      oldContent: Seq[common.ArticleContent],
      isNewLanguage: Boolean,
  ): Try[Option[String]] = {
    // Cloning files if they exist in other languages when adding new language
    if (isNewLanguage) {
      content.traverse(updContent => {
        cloneFilesIfExists(oldContent.map(_.content), updContent)
      })
    } else Success(content)
  }

  private def getNewEditorialNotes(
      isNewLanguage: Boolean,
      user: TokenUser,
      article: api.UpdatedArticleDTO,
      toMergeInto: Draft,
  ): Try[Seq[common.EditorNote]] = {
    val newLanguageEditorNote =
      if (isNewLanguage) Seq(s"Ny språkvariant '${article.language.getOrElse("und")}' ble lagt til.")
      else Seq.empty

    val changedResponsible = article.responsibleId match {
      case UpdateWith(newId) if !toMergeInto.responsible.map(_.responsibleId).contains(newId) =>
        Seq("Ansvarlig endret.")
      case _ => Seq.empty
    }
    val allNewNotes = newLanguageEditorNote ++ changedResponsible

    val addedNotes = article.notes match {
      case Some(n) => newNotes(n ++ allNewNotes, user, toMergeInto.status)
      case None    => newNotes(allNewNotes, user, toMergeInto.status)
    }

    addedNotes.map(n => toMergeInto.notes ++ n)
  }

  private def getNewResponsible(toMergeInto: Draft, article: UpdatedArticleDTO) =
    (article.responsibleId, toMergeInto.responsible) match {
      case (Delete, _)                                                                            => None
      case (UpdateWith(responsibleId), None)                                                      => Some(Responsible(responsibleId, clock.now()))
      case (UpdateWith(responsibleId), Some(existing)) if existing.responsibleId != responsibleId =>
        Some(Responsible(responsibleId, clock.now()))
      case (_, existing) => existing
    }

  private def getNewPriority(toMergeInto: Draft, article: UpdatedArticleDTO) = article
    .priority
    .getOrElse(toMergeInto.priority)

  private def getNewMetaImage(toMergeInto: Draft, maybeLang: Option[String], updatedArticle: UpdatedArticleDTO) =
    maybeLang
      .map(lang =>
        updatedArticle.metaImage match {
          case Delete        => toMergeInto.metaImage.filterNot(_.language == lang)
          case Missing       => toMergeInto.metaImage
          case UpdateWith(m) =>
            val domainMetaImage = Seq(common.ArticleMetaImage(m.id, m.alt, lang))
            mergeLanguageFields(toMergeInto.metaImage, domainMetaImage)
        }
      )
      .getOrElse(toMergeInto.metaImage)

  def toDomainArticle(toMergeInto: Draft, article: api.UpdatedArticleDTO, user: TokenUser): Try[Draft] = permitTry {
    if (article.language.isEmpty && languageFieldIsDefined(article))
      Failure(ValidationException("language", "This field must be specified when updating language fields")).?

    val isNewLanguage       = article.language.exists(l => !toMergeInto.supportedLanguages.contains(l))
    val createdDate         = toMergeInto.created
    val updatedDate         = clock.now()
    val publishedDate       = article.published.getOrElse(toMergeInto.published)
    val updatedAvailability = common.Availability.valueOf(article.availability).getOrElse(toMergeInto.availability)
    val updatedRevision     = article
      .revisionMeta
      .map(_.map(commonConverter.revisionMetaApiToDomain))
      .getOrElse(toMergeInto.revisionMeta)
    val responsible        = getNewResponsible(toMergeInto, article)
    val copyright          = article.copyright.map(toDomainCopyright).orElse(toMergeInto.copyright)
    val priority           = getNewPriority(toMergeInto, article)
    val newNotes           = getNewEditorialNotes(isNewLanguage, user, article, toMergeInto).?
    val newContent         = cloneFilesForOtherLanguages(article.content, toMergeInto.content, isNewLanguage).?
    val updatedRelatedCont = article.relatedContent.map(toDomainRelatedContent).getOrElse(toMergeInto.relatedContent)
    val reqLibs            = article
      .requiredLibraries
      .map(_.map(toDomainRequiredLibraries))
      .getOrElse(toMergeInto.requiredLibraries)
    val updatedComments = article
      .comments
      .map(comments => commonConverter.mergeUpdatedCommentsWithExisting(comments, toMergeInto.comments))
      .getOrElse(toMergeInto.comments)

    val articleWithNewContent = article.copy(content = newContent)

    val maybeLang        = articleWithNewContent.language
    val updatedMetaImage = getNewMetaImage(toMergeInto, maybeLang, articleWithNewContent)
    val updatedTitles    = mergeLanguageFields(
      toMergeInto.title,
      maybeLang
        .traverse(lang => articleWithNewContent.title.toSeq.map(t => toDomainTitle(api.ArticleTitleDTO(t, t, lang))))
        .flatten,
    )

    val updatedDisclaimer = toMergeInto.disclaimer.withOptValue(articleWithNewContent.disclaimer, maybeLang)

    val updatedContents = mergeLanguageFields(
      toMergeInto.content,
      maybeLang
        .traverse(lang => articleWithNewContent.content.toSeq.map(c => toDomainContent(api.ArticleContentDTO(c, lang))))
        .flatten,
    )
    val updatedTags = mergeLanguageFields(
      toMergeInto.tags,
      maybeLang
        .traverse(lang => articleWithNewContent.tags.flatMap(tags => toDomainTag(Some(tags), lang)).toSeq)
        .flatten,
    )
    val updatedVisualElement = mergeLanguageFields(
      toMergeInto.visualElement,
      maybeLang
        .traverse(lang => articleWithNewContent.visualElement.map(c => toDomainVisualElement(c, lang)).toSeq)
        .flatten,
    )
    val updatedIntroductions = mergeLanguageFields(
      toMergeInto.introduction,
      maybeLang
        .traverse(lang => articleWithNewContent.introduction.map(i => toDomainIntroduction(i, lang)).toSeq)
        .flatten,
    )
    val updatedMetaDescriptions = mergeLanguageFields(
      toMergeInto.metaDescription,
      maybeLang
        .traverse(lang => articleWithNewContent.metaDescription.map(m => toDomainMetaDescription(m, lang)).toSeq)
        .flatten,
    )

    val converted = Draft(
      id = toMergeInto.id,
      revision = Option(article.revision),
      status = toMergeInto.status,
      title = updatedTitles,
      content = updatedContents,
      copyright = copyright,
      tags = updatedTags,
      requiredLibraries = reqLibs,
      visualElement = updatedVisualElement,
      introduction = updatedIntroductions,
      metaDescription = updatedMetaDescriptions,
      metaImage = updatedMetaImage,
      created = createdDate,
      updated = updatedDate,
      updatedBy = user.id,
      published = publishedDate,
      articleType = article.articleType.map(common.ArticleType.valueOfOrError).getOrElse(toMergeInto.articleType),
      notes = newNotes,
      previousVersionsNotes = toMergeInto.previousVersionsNotes,
      editorLabels = article.editorLabels.getOrElse(toMergeInto.editorLabels),
      grepCodes = article.grepCodes.getOrElse(toMergeInto.grepCodes),
      conceptIds = article.conceptIds.getOrElse(toMergeInto.conceptIds),
      availability = updatedAvailability,
      relatedContent = updatedRelatedCont,
      revisionMeta = updatedRevision,
      responsible = responsible,
      slug = article.slug.orElse(toMergeInto.slug),
      comments = updatedComments,
      priority = priority,
      started = toMergeInto.started,
      qualityEvaluation = qualityEvaluationToDomain(article.qualityEvaluation),
      disclaimer = updatedDisclaimer,
      traits = traitUtil.getArticleTraits(updatedContents),
    )

    Success(converted)
  }

  def toApiArticleGrepCodes(result: domain.LanguagelessSearchResult[String]): api.GrepCodesSearchResultDTO = {
    api.GrepCodesSearchResultDTO(result.totalCount, result.page.getOrElse(1), result.pageSize, result.results)
  }

  def addNote(article: Draft, noteText: String, user: TokenUser): Draft = {
    article.copy(notes = article.notes :+ common.EditorNote(noteText, user.id, article.status, clock.now()))
  }

}

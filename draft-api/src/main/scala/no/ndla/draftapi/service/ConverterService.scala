/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.model.{domain => common}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.draft.ArticleStatus
import no.ndla.common.model.domain.draft.ArticleStatus.{DRAFT, IMPORTED}
import no.ndla.common.{Clock, DateParser}
import no.ndla.draftapi.Props
import no.ndla.draftapi.auth.UserInfo
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{NewAgreement, NotFoundException}
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.language.Language.{AllLanguages, UnknownLanguage, findByLanguageOrBestEffort, mergeLanguageFields}
import no.ndla.mapping.License.getLicense
import no.ndla.validation._
import org.jsoup.nodes.Element

import java.time.LocalDateTime
import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with DraftRepository with ArticleApiClient with StateTransitionRules with WriteService with Props =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    import props.{externalApiUrls, resourceHtmlEmbedTag}

    def toDomainArticle(
        newArticleId: Long,
        newArticle: api.NewArticle,
        externalIds: List[String],
        user: UserInfo,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime]
    ): Try[common.draft.Draft] = {
      val domainTitles = Seq(common.ArticleTitle(newArticle.title, newArticle.language))
      val domainContent = newArticle.content
        .map(content => common.ArticleContent(removeUnknownEmbedTagAttributes(content), newArticle.language))
        .toSeq

      val status = externalIds match {
        case Nil => common.Status(DRAFT, Set.empty)
        case _   => common.Status(DRAFT, Set(IMPORTED))
      }

      val newAvailability = common.Availability.valueOf(newArticle.availability).getOrElse(common.Availability.everyone)
      val revisionMeta = newArticle.revisionMeta match {
        case Some(revs) if revs.nonEmpty =>
          newArticle.revisionMeta.map(_.map(toDomainRevisionMeta)).getOrElse(common.draft.RevisionMeta.default)
        case _ => common.draft.RevisionMeta.default
      }

      newNotes(newArticle.notes, user, status).map(notes =>
        common.draft.Draft(
          id = Some(newArticleId),
          revision = None,
          status,
          title = domainTitles,
          content = domainContent.filterNot(_.isEmpty),
          copyright = newArticle.copyright.map(toDomainCopyright),
          tags = toDomainTag(newArticle.tags, newArticle.language).toSeq,
          requiredLibraries = newArticle.requiredLibraries.map(toDomainRequiredLibraries),
          visualElement =
            newArticle.visualElement.map(visual => toDomainVisualElement(visual, newArticle.language)).toSeq,
          introduction = newArticle.introduction
            .map(intro => toDomainIntroduction(intro, newArticle.language))
            .filterNot(_.isEmpty)
            .toSeq,
          metaDescription = newArticle.metaDescription
            .map(meta => toDomainMetaDescription(meta, newArticle.language))
            .filterNot(_.isEmpty)
            .toSeq,
          metaImage =
            newArticle.metaImage.map(meta => toDomainMetaImage(meta, newArticle.language)).filterNot(_.isEmpty).toSeq,
          created = oldNdlaCreatedDate.getOrElse(clock.now()),
          updated = oldNdlaUpdatedDate.getOrElse(clock.now()),
          updatedBy = user.id,
          published = oldNdlaUpdatedDate.getOrElse(
            newArticle.published.getOrElse(clock.now())
          ), // If import use old updated. Else use new published or now
          articleType = common.draft.ArticleType.valueOfOrError(newArticle.articleType),
          notes = notes,
          previousVersionsNotes = Seq.empty,
          editorLabels = newArticle.editorLabels,
          grepCodes = newArticle.grepCodes,
          conceptIds = newArticle.conceptIds,
          availability = newAvailability,
          relatedContent = toDomainRelatedContent(newArticle.relatedContent),
          revisionMeta = revisionMeta
        )
      )
    }

    private def toDomainRevisionMeta(revisionMeta: api.RevisionMeta): common.draft.RevisionMeta = {
      common.draft.RevisionMeta(
        id = revisionMeta.id.map(UUID.fromString).getOrElse(UUID.randomUUID()),
        revisionDate = revisionMeta.revisionDate,
        note = revisionMeta.note,
        status = common.draft.RevisionStatus.fromStringDefault(revisionMeta.status)
      )
    }

    private def toApiRevisionMeta(revisionMeta: common.draft.RevisionMeta): api.RevisionMeta = {
      api.RevisionMeta(
        id = Some(revisionMeta.id.toString),
        revisionDate = revisionMeta.revisionDate,
        note = revisionMeta.note,
        status = revisionMeta.status.entryName
      )
    }

    private[service] def newNotes(
        notes: Seq[String],
        user: UserInfo,
        status: common.Status
    ): Try[Seq[common.EditorNote]] = {
      notes match {
        case Nil                  => Success(Seq.empty)
        case l if !l.contains("") => Success(l.map(common.EditorNote(_, user.id, status, clock.now())))
        case _                    => Failure(ValidationException("notes", "A note can not be an empty string"))
      }
    }

    def toDomainRelatedContent(relatedContent: Seq[api.RelatedContent]): Seq[common.RelatedContent] = {
      relatedContent.map {
        case Left(x)  => Left(common.RelatedContentLink(url = x.url, title = x.title))
        case Right(x) => Right(x)
      }

    }

    def toDomainAgreement(newAgreement: NewAgreement, user: UserInfo): domain.Agreement = {
      domain.Agreement(
        id = None,
        title = newAgreement.title,
        content = newAgreement.content,
        copyright = toDomainCopyright(newAgreement.copyright),
        created = clock.now(),
        updated = clock.now(),
        updatedBy = user.id
      )
    }

    def toDomainTitle(articleTitle: api.ArticleTitle): common.ArticleTitle =
      common.ArticleTitle(articleTitle.title, articleTitle.language)

    def toDomainContent(articleContent: api.ArticleContent): common.ArticleContent = {
      common.ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.language)
    }

    def toDomainTag(tag: api.ArticleTag): common.ArticleTag = common.ArticleTag(tag.tags, tag.language)

    def toDomainTag(tag: Seq[String], language: String): Option[common.ArticleTag] =
      if (tag.nonEmpty) Some(common.ArticleTag(tag, language)) else None

    def toDomainVisualElement(visual: api.VisualElement): common.VisualElement = {
      common.VisualElement(removeUnknownEmbedTagAttributes(visual.visualElement), visual.language)
    }

    def toDomainVisualElement(visual: String, language: String): common.VisualElement =
      common.VisualElement(removeUnknownEmbedTagAttributes(visual), language)

    def toDomainIntroduction(intro: api.ArticleIntroduction): common.ArticleIntroduction = {
      common.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toDomainIntroduction(intro: String, language: String): common.ArticleIntroduction =
      common.ArticleIntroduction(intro, language)

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): common.ArticleMetaDescription = {
      common.ArticleMetaDescription(meta.metaDescription, meta.language)
    }

    def toDomainMetaDescription(meta: String, language: String): common.ArticleMetaDescription =
      common.ArticleMetaDescription(meta, language)

    def toDomainMetaImage(metaImage: api.NewArticleMetaImage, language: String): common.ArticleMetaImage =
      common.ArticleMetaImage(metaImage.id, metaImage.alt, language)

    def toDomainCopyright(newCopyright: api.NewAgreementCopyright): common.draft.Copyright = {
      val validFrom = newCopyright.validFrom.map(date => DateParser.fromString(date))
      val validTo   = newCopyright.validTo.map(date => DateParser.fromString(date))

      val apiCopyright = api.Copyright(
        newCopyright.license,
        newCopyright.origin,
        newCopyright.creators,
        newCopyright.processors,
        newCopyright.rightsholders,
        newCopyright.agreementId,
        validFrom,
        validTo
      )
      toDomainCopyright(apiCopyright)
    }

    def toDomainCopyright(copyright: api.Copyright): common.draft.Copyright = {
      common.draft.Copyright(
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

    def getEmbeddedConceptIds(article: common.draft.Draft): Seq[Long] = {
      val htmlElements = article.content.map(content => HtmlTagRules.stringToJsoupDocument(content.content))
      val conceptEmbeds = htmlElements.flatMap(elem => {
        val conceptSelector = s"$resourceHtmlEmbedTag[${TagAttributes.DataResource}=${ResourceType.Concept}]"
        elem.select(conceptSelector).asScala.toSeq
      })

      val conceptIds = conceptEmbeds.flatMap(embed => {
        Try(embed.attr(TagAttributes.DataContentId.toString).toLong) match {
          case Failure(ex) =>
            logger.error(s"Could not derive concept id from embed: '${embed.toString}'", ex)
            None
          case Success(id) => Some(id)
        }
      })
      conceptIds
    }

    def getEmbeddedH5PPaths(article: common.draft.Draft): Seq[String] = {
      val getH5PEmbeds = (htmlElements: Seq[Element]) => {
        htmlElements.flatMap(elem => {
          val h5pSelector = s"$resourceHtmlEmbedTag[${TagAttributes.DataResource}=${ResourceType.H5P}]"
          elem.select(h5pSelector).asScala.toSeq
        })
      }

      val htmlElements   = article.content.map(content => HtmlTagRules.stringToJsoupDocument(content.content))
      val visualElements = article.visualElement.map(ve => HtmlTagRules.stringToJsoupDocument(ve.resource))

      val h5pEmbeds = getH5PEmbeds(htmlElements) ++ getH5PEmbeds(visualElements)

      h5pEmbeds.flatMap(embed => {
        Try(embed.attr(TagAttributes.DataPath.toString)) match {
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

    def toDomainAuthor(author: api.Author): common.Author = common.Author(author.`type`, author.name)

    def toDomainRequiredLibraries(requiredLibs: api.RequiredLibrary): common.RequiredLibrary = {
      common.RequiredLibrary(requiredLibs.mediaType, requiredLibs.name, requiredLibs.url)
    }

    private def getLinkToOldNdla(id: Long): Option[String] =
      draftRepository.getExternalIdsFromId(id).map(createLinkToOldNdla).headOption

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

    def updateStatus(
        status: ArticleStatus.Value,
        article: common.draft.Draft,
        user: UserInfo,
        isImported: Boolean
    ): IO[Try[common.draft.Draft]] = StateTransitionRules.doTransition(article, status, user, isImported)

    def toApiArticle(article: common.draft.Draft, language: String, fallback: Boolean = false): Try[api.Article] = {
      val isLanguageNeutral =
        article.supportedLanguages.contains(UnknownLanguage.toString) && article.supportedLanguages.length == 1

      if (article.supportedLanguages.contains(language) || language == AllLanguages || isLanguageNeutral || fallback) {
        val metaDescription =
          findByLanguageOrBestEffort(article.metaDescription, language).map(toApiArticleMetaDescription)
        val tags           = findByLanguageOrBestEffort(article.tags, language).map(toApiArticleTag)
        val title          = findByLanguageOrBestEffort(article.title, language).map(toApiArticleTitle)
        val introduction   = findByLanguageOrBestEffort(article.introduction, language).map(toApiArticleIntroduction)
        val visualElement  = findByLanguageOrBestEffort(article.visualElement, language).map(toApiVisualElement)
        val articleContent = findByLanguageOrBestEffort(article.content, language).map(toApiArticleContent)
        val metaImage      = findByLanguageOrBestEffort(article.metaImage, language).map(toApiArticleMetaImage)
        val revisionMetas  = article.revisionMeta.map(toApiRevisionMeta)

        Success(
          api.Article(
            id = article.id.get,
            oldNdlaUrl = article.id.flatMap(getLinkToOldNdla),
            revision = article.revision.get,
            status = toApiStatus(article.status),
            title = title,
            content = articleContent,
            copyright = article.copyright.map(toApiCopyright),
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
            revisions = revisionMetas
          )
        )
      } else {
        Failure(
          NotFoundException(
            s"The article with id ${article.id.get} and language $language was not found",
            article.supportedLanguages
          )
        )
      }
    }

    def toApiAgreement(agreement: domain.Agreement): api.Agreement = {
      api.Agreement(
        id = agreement.id.get,
        title = agreement.title,
        content = agreement.content,
        copyright = toApiCopyright(agreement.copyright),
        created = agreement.created,
        updated = agreement.updated,
        updatedBy = agreement.updatedBy
      )
    }

    def toApiUserData(userData: domain.UserData): api.UserData = {
      api.UserData(
        userId = userData.userId,
        savedSearches = userData.savedSearches,
        latestEditedArticles = userData.latestEditedArticles,
        favoriteSubjects = userData.favoriteSubjects
      )
    }

    def toApiEditorNote(note: common.EditorNote): api.EditorNote =
      api.EditorNote(note.note, note.user, toApiStatus(note.status), note.timestamp)

    def toApiStatus(status: common.Status): api.Status =
      api.Status(status.current.toString, status.other.map(_.toString).toSeq)

    def toApiArticleTitle(title: common.ArticleTitle): api.ArticleTitle = api.ArticleTitle(title.title, title.language)

    def toApiArticleContent(content: common.ArticleContent): api.ArticleContent =
      api.ArticleContent(content.content, content.language)

    def toApiArticleMetaImage(metaImage: common.ArticleMetaImage): api.ArticleMetaImage = {
      api.ArticleMetaImage(
        s"${externalApiUrls("raw-image")}/${metaImage.imageId}",
        metaImage.altText,
        metaImage.language
      )
    }

    def toApiCopyright(copyright: common.draft.Copyright): api.Copyright = {
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

    def toApiAuthor(author: common.Author): api.Author = api.Author(author.`type`, author.name)

    def toApiRelatedContent(relatedContent: common.RelatedContent): api.RelatedContent = {
      relatedContent match {
        case Left(x)  => Left(api.RelatedContentLink(url = x.url, title = x.title))
        case Right(x) => Right(x)
      }

    }

    def toApiArticleTag(tag: common.ArticleTag): api.ArticleTag = api.ArticleTag(tag.tags, tag.language)

    def toApiRequiredLibrary(required: common.RequiredLibrary): api.RequiredLibrary = {
      api.RequiredLibrary(required.mediaType, required.name, required.url)
    }

    def toApiVisualElement(visual: common.VisualElement): api.VisualElement =
      api.VisualElement(visual.resource, visual.language)

    def toApiArticleIntroduction(intro: common.ArticleIntroduction): api.ArticleIntroduction = {
      api.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticleMetaDescription(metaDescription: common.ArticleMetaDescription): api.ArticleMetaDescription = {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def toArticleApiCopyright(copyright: common.draft.Copyright): api.ArticleApiCopyright = {
      def toArticleApiAuthor(author: common.Author): api.ArticleApiAuthor =
        api.ArticleApiAuthor(author.`type`, author.name)

      api.ArticleApiCopyright(
        copyright.license.getOrElse(""),
        copyright.origin.getOrElse(""),
        copyright.creators.map(toArticleApiAuthor),
        copyright.processors.map(toArticleApiAuthor),
        copyright.rightsholders.map(toArticleApiAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def deleteLanguage(
        article: common.draft.Draft,
        language: String,
        userInfo: UserInfo
    ): Try[common.draft.Draft] = {
      val title               = article.title.filter(_.language != language)
      val content             = article.content.filter(_.language != language)
      val articleIntroduction = article.introduction.filter(_.language != language)
      val metaDescription     = article.metaDescription.filter(_.language != language)
      val tags                = article.tags.filter(_.language != language)
      val metaImage           = article.metaImage.filter(_.language != language)
      val visualElement       = article.visualElement.filter(_.language != language)
      newNotes(Seq(s"Slettet språkvariant '$language'."), userInfo, article.status) match {
        case Failure(ex) => Failure(ex)
        case Success(newEditorNotes) =>
          Success(
            article.copy(
              title = title,
              content = content,
              introduction = articleIntroduction,
              metaDescription = metaDescription,
              tags = tags,
              metaImage = metaImage,
              visualElement = visualElement,
              notes = article.notes ++ newEditorNotes
            )
          )
      }
    }

    def getNextRevision(article: common.draft.Draft): Option[common.draft.RevisionMeta] = getNextRevision(
      article.revisionMeta
    )
    def getNextRevision(revisions: Seq[common.draft.RevisionMeta]): Option[common.draft.RevisionMeta] =
      revisions.filterNot(_.status == common.draft.RevisionStatus.Revised).sortBy(_.revisionDate).headOption

    def toArticleApiArticle(article: common.draft.Draft): api.ArticleApiArticle = {
      api.ArticleApiArticle(
        revision = article.revision,
        title = article.title.map(t => api.ArticleApiTitle(t.title, t.language)),
        content = article.content.map(c => api.ArticleApiContent(c.content, c.language)),
        copyright = article.copyright.map(toArticleApiCopyright),
        tags = article.tags.map(t => api.ArticleApiTag(t.tags, t.language)),
        requiredLibraries =
          article.requiredLibraries.map(r => api.ArticleApiRequiredLibrary(r.mediaType, r.name, r.url)),
        visualElement = article.visualElement.map(v => api.ArticleApiVisualElement(v.resource, v.language)),
        introduction = article.introduction.map(i => api.ArticleApiIntroduction(i.introduction, i.language)),
        metaDescription = article.metaDescription.map(m => api.ArticleApiMetaDescription(m.content, m.language)),
        metaImage = article.metaImage.map(m => api.ArticleApiMetaImage(m.imageId, m.altText, m.language)),
        created = article.created,
        updated = article.updated,
        updatedBy = article.updatedBy,
        published = article.published,
        articleType = article.articleType.entryName,
        grepCodes = article.grepCodes,
        conceptIds = article.conceptIds,
        availability = article.availability,
        relatedContent = article.relatedContent.map(toApiRelatedContent),
        revisionDate = getNextRevision(article.revisionMeta).map(_.revisionDate)
      )
    }

    private def languageFieldIsDefined(article: api.UpdatedArticle): Boolean = {
      val metaImageExists = article.metaImage.map(_.isDefined).getOrElse(true)
      val langFields: Seq[Option[_]] = Seq(
        article.title,
        article.content,
        article.tags,
        article.introduction,
        article.metaDescription,
        article.visualElement
      )

      langFields.foldRight(false)((curr, res) => res || curr.isDefined || metaImageExists)
    }

    private def getExistingPaths(content: String): Seq[String] = {
      val doc        = HtmlTagRules.stringToJsoupDocument(content)
      val fileEmbeds = doc.select(s"embed[${TagAttributes.DataResource}='${ResourceType.File}']").asScala.toSeq
      fileEmbeds.flatMap(e => Option(e.attr(TagAttributes.DataPath.toString)))
    }

    def cloneFilesIfExists(existingContent: Seq[String], newContent: String): Try[String] = {
      val existingFiles = existingContent.flatMap(getExistingPaths)

      val doc        = HtmlTagRules.stringToJsoupDocument(newContent)
      val fileEmbeds = doc.select(s"embed[${TagAttributes.DataResource}='${ResourceType.File}']").asScala

      val embedsToCloneFile = fileEmbeds.filter(embed => {
        Option(embed.attr(TagAttributes.DataPath.toString)).exists(dataPath => existingFiles.contains(dataPath))
      })

      val cloned = embedsToCloneFile.toList
        .map(writeService.cloneEmbedAndUpdateElement)
        .sequence

      cloned match {
        case Failure(ex) => Failure(ex)
        case Success(_)  => Success(HtmlTagRules.jsoupDocumentToString(doc))
      }
    }

    private def cloneFilesForOtherLanguages(
        content: Option[String],
        oldContent: Seq[common.ArticleContent],
        isNewLanguage: Boolean
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
        user: UserInfo,
        article: api.UpdatedArticle,
        toMergeInto: common.draft.Draft
    ): Try[Seq[common.EditorNote]] = {
      val newLanguageEditorNote =
        if (isNewLanguage) Seq(s"Ny språkvariant '${article.language.getOrElse("und")}' ble lagt til.")
        else Seq.empty

      val addedNotes = article.notes match {
        case Some(n) => newNotes(n ++ newLanguageEditorNote, user, toMergeInto.status)
        case None    => newNotes(newLanguageEditorNote, user, toMergeInto.status)
      }

      addedNotes.map(n => toMergeInto.notes ++ n)
    }

    def toDomainArticle(
        toMergeInto: common.draft.Draft,
        article: api.UpdatedArticle,
        isImported: Boolean,
        user: UserInfo,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime]
    ): Try[common.draft.Draft] = {
      val isNewLanguage = article.language.exists(l => !toMergeInto.supportedLanguages.contains(l))
      val createdDate   = if (isImported) oldNdlaCreatedDate.getOrElse(toMergeInto.created) else toMergeInto.created
      val updatedDate   = if (isImported) oldNdlaUpdatedDate.getOrElse(clock.now()) else clock.now()
      val publishedDate = article.published.getOrElse(toMergeInto.published)
      val updatedAvailability = common.Availability.valueOf(article.availability).getOrElse(toMergeInto.availability)
      val updatedRevisionMeta =
        article.revisionMeta.map(_.map(toDomainRevisionMeta)).getOrElse(toMergeInto.revisionMeta)

      val updatedRequiredLibraries = article.requiredLibraries
        .map(_.map(toDomainRequiredLibraries))
        .getOrElse(toMergeInto.requiredLibraries)

      val updatedRelatedContent = article.relatedContent
        .map(toDomainRelatedContent)
        .getOrElse(toMergeInto.relatedContent)

      val failableFields = for {
        newNotes   <- getNewEditorialNotes(isNewLanguage, user, article, toMergeInto)
        newContent <- cloneFilesForOtherLanguages(article.content, toMergeInto.content, isNewLanguage)
      } yield (newNotes, newContent)

      failableFields match {
        case Failure(ex) => Failure(ex)
        case Success((allNotes, newContent)) =>
          val partiallyConverted = toMergeInto.copy(
            revision = Option(article.revision),
            copyright = article.copyright.map(toDomainCopyright).orElse(toMergeInto.copyright),
            requiredLibraries = updatedRequiredLibraries,
            created = createdDate,
            updated = updatedDate,
            published = publishedDate,
            updatedBy = user.id,
            articleType =
              article.articleType.map(common.draft.ArticleType.valueOfOrError).getOrElse(toMergeInto.articleType),
            notes = allNotes,
            editorLabels = article.editorLabels.getOrElse(toMergeInto.editorLabels),
            grepCodes = article.grepCodes.getOrElse(toMergeInto.grepCodes),
            conceptIds = article.conceptIds.getOrElse(toMergeInto.conceptIds),
            availability = updatedAvailability,
            relatedContent = updatedRelatedContent,
            revisionMeta = updatedRevisionMeta
          )

          val articleWithNewContent = article.copy(content = newContent)
          articleWithNewContent.language match {
            case None if languageFieldIsDefined(articleWithNewContent) =>
              Failure(ValidationException("language", "This field must be specified when updating language fields"))
            case None       => Success(partiallyConverted)
            case Some(lang) => Success(mergeArticleLanguageFields(partiallyConverted, articleWithNewContent, lang))
          }
      }
    }

    private[service] def mergeArticleLanguageFields(
        toMergeInto: common.draft.Draft,
        updatedArticle: api.UpdatedArticle,
        lang: String
    ): common.draft.Draft = {
      val updatedTitles           = updatedArticle.title.toSeq.map(t => toDomainTitle(api.ArticleTitle(t, lang)))
      val updatedContents         = updatedArticle.content.toSeq.map(c => toDomainContent(api.ArticleContent(c, lang)))
      val updatedTags             = updatedArticle.tags.flatMap(tags => toDomainTag(tags, lang)).toSeq
      val updatedVisualElement    = updatedArticle.visualElement.map(c => toDomainVisualElement(c, lang)).toSeq
      val updatedIntroductions    = updatedArticle.introduction.map(i => toDomainIntroduction(i, lang)).toSeq
      val updatedMetaDescriptions = updatedArticle.metaDescription.map(m => toDomainMetaDescription(m, lang)).toSeq

      val updatedMetaImage = updatedArticle.metaImage match {
        case Left(_) => toMergeInto.metaImage.filterNot(_.language == lang)
        case Right(meta) =>
          val domainMetaImage = meta
            .map(m => common.ArticleMetaImage(m.id, m.alt, lang))
            .toSeq
          mergeLanguageFields(toMergeInto.metaImage, domainMetaImage)
      }

      toMergeInto.copy(
        title = mergeLanguageFields(toMergeInto.title, updatedTitles),
        content = mergeLanguageFields(toMergeInto.content, updatedContents),
        tags = mergeLanguageFields(toMergeInto.tags, updatedTags),
        visualElement = mergeLanguageFields(toMergeInto.visualElement, updatedVisualElement),
        introduction = mergeLanguageFields(toMergeInto.introduction, updatedIntroductions),
        metaDescription = mergeLanguageFields(toMergeInto.metaDescription, updatedMetaDescriptions),
        metaImage = updatedMetaImage
      )
    }

    def toDomainArticle(
        id: Long,
        article: api.UpdatedArticle,
        isImported: Boolean,
        user: UserInfo,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime]
    ): Try[common.draft.Draft] = {
      val createdDate = oldNdlaCreatedDate.getOrElse(clock.now())
      val updatedDate = oldNdlaUpdatedDate.getOrElse(clock.now())

      article.language match {
        case None =>
          val error = ValidationMessage("language", "This field must be specified when updating language fields")
          Failure(new ValidationException(errors = Seq(error)))
        case Some(lang) =>
          val status =
            if (isImported) common.Status(DRAFT, Set(IMPORTED))
            else common.Status(DRAFT, Set.empty)

          val mergedNotes = article.notes.map(n => newNotes(n, user, status)) match {
            case Some(Failure(ex))    => Failure(ex)
            case Some(Success(notes)) => Success(notes)
            case None                 => Success(Seq.empty)
          }

          val newMetaImage = article.metaImage match {
            case Right(meta) => meta.map(m => common.ArticleMetaImage(m.id, m.alt, lang)).toSeq
            case Left(_)     => Seq.empty
          }

          val updatedAvailability =
            common.Availability.valueOf(article.availability).getOrElse(common.Availability.everyone)
          val updatedRevisionMeta = article.revisionMeta.toSeq.flatMap(_.map(toDomainRevisionMeta))

          mergedNotes.map(notes =>
            common.draft.Draft(
              id = Some(id),
              revision = Some(1),
              status = status,
              title = article.title.map(t => common.ArticleTitle(t, lang)).toSeq,
              content = article.content.map(c => common.ArticleContent(c, lang)).toSeq,
              copyright = article.copyright.map(toDomainCopyright),
              tags = article.tags.toSeq.map(tags => common.ArticleTag(tags, lang)),
              requiredLibraries = article.requiredLibraries.map(_.map(toDomainRequiredLibraries)).toSeq.flatten,
              visualElement = article.visualElement.map(v => toDomainVisualElement(v, lang)).toSeq,
              introduction = article.introduction.map(i => toDomainIntroduction(i, lang)).toSeq,
              metaDescription = article.metaDescription.map(m => toDomainMetaDescription(m, lang)).toSeq,
              metaImage = newMetaImage,
              created = createdDate,
              updated = updatedDate,
              published = article.published.getOrElse(clock.now()),
              updatedBy = user.id,
              articleType = article.articleType
                .map(common.draft.ArticleType.valueOfOrError)
                .getOrElse(common.draft.ArticleType.Standard),
              notes = notes,
              previousVersionsNotes = Seq.empty,
              editorLabels = article.editorLabels.getOrElse(Seq.empty),
              grepCodes = article.grepCodes.getOrElse(Seq.empty),
              conceptIds = article.conceptIds.getOrElse(Seq.empty),
              availability = updatedAvailability,
              relatedContent = article.relatedContent.map(toDomainRelatedContent).getOrElse(Seq.empty),
              revisionMeta = updatedRevisionMeta
            )
          )
      }
    }

    private[service] def _stateTransitionsToApi(
        user: UserInfo,
        article: Option[common.draft.Draft]
    ): Map[String, Seq[String]] = {
      StateTransitionRules.StateTransitions.groupBy(_.from).map { case (from, to) =>
        from.toString -> to
          .filter(_.hasRequiredRoles(user, article))
          .map(_.to.toString)
          .toSeq
      }
    }

    def stateTransitionsToApi(user: UserInfo, articleId: Option[Long]): Try[Map[String, Seq[String]]] =
      articleId match {
        case Some(id) =>
          draftRepository.withId(id) match {
            case Some(article) => Success(_stateTransitionsToApi(user, Some(article)))
            case None          => Failure(NotFoundException("The article does not exist"))
          }
        case None => Success(_stateTransitionsToApi(user, None))
      }

    def toApiArticleGrepCodes(result: domain.LanguagelessSearchResult[String]): api.GrepCodesSearchResult = {
      api.GrepCodesSearchResult(result.totalCount, result.page.getOrElse(1), result.pageSize, result.results)
    }

    def addNote(article: common.draft.Draft, noteText: String, user: UserInfo): common.draft.Draft = {
      article.copy(
        notes = article.notes :+ common.EditorNote(noteText, user.id, article.status, clock.now())
      )
    }

  }

}

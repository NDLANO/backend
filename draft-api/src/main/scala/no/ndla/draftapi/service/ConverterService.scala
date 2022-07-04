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
import no.ndla.common.DateParser
import no.ndla.draftapi.Props
import no.ndla.draftapi.auth.UserInfo
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{NewAgreement, NotFoundException}
import no.ndla.draftapi.model.domain.ArticleStatus._
import no.ndla.draftapi.model.domain._
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
    ): Try[domain.Article] = {
      val domainTitles = Seq(domain.ArticleTitle(newArticle.title, newArticle.language))
      val domainContent = newArticle.content
        .map(content => domain.ArticleContent(removeUnknownEmbedTagAttributes(content), newArticle.language))
        .toSeq

      val status = externalIds match {
        case Nil => domain.Status(DRAFT, Set.empty)
        case _   => domain.Status(DRAFT, Set(IMPORTED))
      }

      val newAvailability = Availability.valueOf(newArticle.availability).getOrElse(Availability.everyone)
      val revisionMeta    = newArticle.revisionMeta.map(_.map(toDomainRevisionMeta)).getOrElse(RevisionMeta.default)

      newNotes(newArticle.notes, user, status).map(notes =>
        domain.Article(
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
          articleType = ArticleType.valueOfOrError(newArticle.articleType),
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

    private def toDomainRevisionMeta(revisionMeta: api.RevisionMeta): domain.RevisionMeta = {
      domain.RevisionMeta(
        id = revisionMeta.id.map(UUID.fromString).getOrElse(UUID.randomUUID()),
        revisionDate = revisionMeta.revisionDate,
        note = revisionMeta.note,
        status = RevisionStatus.fromStringDefault(revisionMeta.status)
      )
    }

    private def toApiRevisionMeta(revisionMeta: domain.RevisionMeta): api.RevisionMeta = {
      api.RevisionMeta(
        id = Some(revisionMeta.id.toString),
        revisionDate = revisionMeta.revisionDate,
        note = revisionMeta.note,
        status = revisionMeta.status.entryName
      )
    }

    private[service] def newNotes(notes: Seq[String], user: UserInfo, status: Status): Try[Seq[EditorNote]] = {
      notes match {
        case Nil                  => Success(Seq.empty)
        case l if !l.contains("") => Success(l.map(domain.EditorNote(_, user.id, status, clock.now())))
        case _ =>
          Failure(
            new ValidationException(errors = Seq(ValidationMessage("notes", "A note can not be an empty string")))
          )
      }
    }

    def toDomainRelatedContent(relatedContent: Seq[api.RelatedContent]): Seq[RelatedContent] = {
      relatedContent.map {
        case Left(x)  => Left(domain.RelatedContentLink(url = x.url, title = x.title))
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

    def toDomainTitle(articleTitle: api.ArticleTitle): domain.ArticleTitle =
      domain.ArticleTitle(articleTitle.title, articleTitle.language)

    def toDomainContent(articleContent: api.ArticleContent): domain.ArticleContent = {
      domain.ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.language)
    }

    def toDomainTag(tag: api.ArticleTag): domain.ArticleTag = domain.ArticleTag(tag.tags, tag.language)

    def toDomainTag(tag: Seq[String], language: String): Option[domain.ArticleTag] =
      if (tag.nonEmpty) Some(domain.ArticleTag(tag, language)) else None

    def toDomainVisualElement(visual: api.VisualElement): domain.VisualElement = {
      domain.VisualElement(removeUnknownEmbedTagAttributes(visual.visualElement), visual.language)
    }

    def toDomainVisualElement(visual: String, language: String): domain.VisualElement =
      domain.VisualElement(removeUnknownEmbedTagAttributes(visual), language)

    def toDomainIntroduction(intro: api.ArticleIntroduction): domain.ArticleIntroduction = {
      domain.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toDomainIntroduction(intro: String, language: String): domain.ArticleIntroduction =
      domain.ArticleIntroduction(intro, language)

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): domain.ArticleMetaDescription = {
      domain.ArticleMetaDescription(meta.metaDescription, meta.language)
    }

    def toDomainMetaDescription(meta: String, language: String): domain.ArticleMetaDescription =
      domain.ArticleMetaDescription(meta, language)

    def toDomainMetaImage(metaImage: api.NewArticleMetaImage, language: String): domain.ArticleMetaImage =
      domain.ArticleMetaImage(metaImage.id, metaImage.alt, language)

    def toDomainCopyright(newCopyright: api.NewAgreementCopyright): domain.Copyright = {
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

    def getEmbeddedConceptIds(article: domain.Article): Seq[Long] = {
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

    def getEmbeddedH5PPaths(article: domain.Article): Seq[String] = {
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

    def toDomainAuthor(author: api.Author): domain.Author = domain.Author(author.`type`, author.name)

    def toDomainRequiredLibraries(requiredLibs: api.RequiredLibrary): domain.RequiredLibrary = {
      domain.RequiredLibrary(requiredLibs.mediaType, requiredLibs.name, requiredLibs.url)
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
        article: domain.Article,
        user: UserInfo,
        isImported: Boolean
    ): IO[Try[domain.Article]] = StateTransitionRules.doTransition(article, status, user, isImported)

    def toApiArticle(article: domain.Article, language: String, fallback: Boolean = false): Try[api.Article] = {
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

    def toApiEditorNote(note: domain.EditorNote): api.EditorNote =
      api.EditorNote(note.note, note.user, toApiStatus(note.status), note.timestamp)

    def toApiStatus(status: domain.Status): api.Status =
      api.Status(status.current.toString, status.other.map(_.toString).toSeq)

    def toApiArticleTitle(title: domain.ArticleTitle): api.ArticleTitle = api.ArticleTitle(title.title, title.language)

    def toApiArticleContent(content: domain.ArticleContent): api.ArticleContent =
      api.ArticleContent(content.content, content.language)

    def toApiArticleMetaImage(metaImage: domain.ArticleMetaImage): api.ArticleMetaImage = {
      api.ArticleMetaImage(
        s"${externalApiUrls("raw-image")}/${metaImage.imageId}",
        metaImage.altText,
        metaImage.language
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

    def toApiAuthor(author: domain.Author): api.Author = api.Author(author.`type`, author.name)

    def toApiRelatedContent(relatedContent: domain.RelatedContent): api.RelatedContent = {
      relatedContent match {
        case Left(x)  => Left(api.RelatedContentLink(url = x.url, title = x.title))
        case Right(x) => Right(x)
      }

    }

    def toApiArticleTag(tag: domain.ArticleTag): api.ArticleTag = api.ArticleTag(tag.tags, tag.language)

    def toApiRequiredLibrary(required: domain.RequiredLibrary): api.RequiredLibrary = {
      api.RequiredLibrary(required.mediaType, required.name, required.url)
    }

    def toApiVisualElement(visual: domain.VisualElement): api.VisualElement =
      api.VisualElement(visual.resource, visual.language)

    def toApiArticleIntroduction(intro: domain.ArticleIntroduction): api.ArticleIntroduction = {
      api.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticleMetaDescription(metaDescription: domain.ArticleMetaDescription): api.ArticleMetaDescription = {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def toArticleApiCopyright(copyright: domain.Copyright): api.ArticleApiCopyright = {
      def toArticleApiAuthor(author: domain.Author): api.ArticleApiAuthor =
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

    def deleteLanguage(article: domain.Article, language: String, userInfo: UserInfo): Try[Article] = {
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

    def getNextRevision(article: domain.Article): Option[domain.RevisionMeta] = getNextRevision(article.revisionMeta)
    def getNextRevision(revisions: Seq[domain.RevisionMeta]): Option[domain.RevisionMeta] =
      revisions.filterNot(_.status == RevisionStatus.Revised).sortBy(_.revisionDate).headOption

    def toArticleApiArticle(article: domain.Article): api.ArticleApiArticle = {
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
        oldContent: Seq[domain.ArticleContent],
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
        toMergeInto: domain.Article
    ): Try[Seq[EditorNote]] = {
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
        toMergeInto: domain.Article,
        article: api.UpdatedArticle,
        isImported: Boolean,
        user: UserInfo,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime]
    ): Try[domain.Article] = {
      val isNewLanguage = article.language.exists(l => !toMergeInto.supportedLanguages.contains(l))
      val createdDate   = if (isImported) oldNdlaCreatedDate.getOrElse(toMergeInto.created) else toMergeInto.created
      val updatedDate   = if (isImported) oldNdlaUpdatedDate.getOrElse(clock.now()) else clock.now()
      val publishedDate = article.published.getOrElse(toMergeInto.published)
      val updatedAvailability = Availability.valueOf(article.availability).getOrElse(toMergeInto.availability)
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
            articleType = article.articleType.map(ArticleType.valueOfOrError).getOrElse(toMergeInto.articleType),
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
              val error = ValidationMessage("language", "This field must be specified when updating language fields")
              Failure(new ValidationException(errors = Seq(error)))
            case None       => Success(partiallyConverted)
            case Some(lang) => Success(mergeArticleLanguageFields(partiallyConverted, articleWithNewContent, lang))
          }
      }
    }

    private[service] def mergeArticleLanguageFields(
        toMergeInto: Article,
        updatedArticle: api.UpdatedArticle,
        lang: String
    ): Article = {
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
            .map(m => domain.ArticleMetaImage(m.id, m.alt, lang))
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
    ): Try[domain.Article] = {
      val createdDate = oldNdlaCreatedDate.getOrElse(clock.now())
      val updatedDate = oldNdlaUpdatedDate.getOrElse(clock.now())

      article.language match {
        case None =>
          val error = ValidationMessage("language", "This field must be specified when updating language fields")
          Failure(new ValidationException(errors = Seq(error)))
        case Some(lang) =>
          val status =
            if (isImported) domain.Status(DRAFT, Set(ArticleStatus.IMPORTED))
            else domain.Status(DRAFT, Set.empty)

          val mergedNotes = article.notes.map(n => newNotes(n, user, status)) match {
            case Some(Failure(ex))    => Failure(ex)
            case Some(Success(notes)) => Success(notes)
            case None                 => Success(Seq.empty)
          }

          val newMetaImage = article.metaImage match {
            case Right(meta) => meta.map(m => domain.ArticleMetaImage(m.id, m.alt, lang)).toSeq
            case Left(_)     => Seq.empty
          }

          val updatedAvailability = Availability.valueOf(article.availability).getOrElse(Availability.everyone)
          val updatedRevisionMeta = article.revisionMeta.toSeq.flatMap(_.map(toDomainRevisionMeta))

          mergedNotes.map(notes =>
            domain.Article(
              id = Some(id),
              revision = Some(1),
              status = status,
              title = article.title.map(t => domain.ArticleTitle(t, lang)).toSeq,
              content = article.content.map(c => domain.ArticleContent(c, lang)).toSeq,
              copyright = article.copyright.map(toDomainCopyright),
              tags = article.tags.toSeq.map(tags => domain.ArticleTag(tags, lang)),
              requiredLibraries = article.requiredLibraries.map(_.map(toDomainRequiredLibraries)).toSeq.flatten,
              visualElement = article.visualElement.map(v => toDomainVisualElement(v, lang)).toSeq,
              introduction = article.introduction.map(i => toDomainIntroduction(i, lang)).toSeq,
              metaDescription = article.metaDescription.map(m => toDomainMetaDescription(m, lang)).toSeq,
              metaImage = newMetaImage,
              created = createdDate,
              updated = updatedDate,
              published = article.published.getOrElse(clock.now()),
              updatedBy = user.id,
              articleType = article.articleType.map(ArticleType.valueOfOrError).getOrElse(ArticleType.Standard),
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

    def toApiAvailability(availability: domain.Availability.Value): api.Availability.Value = {
      availability match {
        case domain.Availability.everyone => api.Availability.everyone
        case domain.Availability.teacher  => api.Availability.teacher
        case _                            => api.Availability.everyone
      }
    }

    private[service] def _stateTransitionsToApi(user: UserInfo, article: Option[Article]): Map[String, Seq[String]] = {
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

    def toApiArticleGrepCodes(result: LanguagelessSearchResult[String]): api.GrepCodesSearchResult = {
      api.GrepCodesSearchResult(result.totalCount, result.page.getOrElse(1), result.pageSize, result.results)
    }

    def addNote(article: domain.Article, noteText: String, user: UserInfo): domain.Article = {
      article.copy(
        notes = article.notes :+ domain.EditorNote(noteText, user.id, article.status, LocalDateTime.now())
      )
    }

  }

}

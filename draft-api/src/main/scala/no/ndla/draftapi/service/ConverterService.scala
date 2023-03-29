/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.{RelatedContentLink, api => commonApi, domain => common}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.Responsible
import no.ndla.common.model.domain.draft.{Comment, Draft, DraftStatus}
import no.ndla.common.model.domain.draft.DraftStatus.{IMPORTED, PLANNED}
import no.ndla.common.{Clock, DateParser, UUIDUtil}
import no.ndla.draftapi.Props
import no.ndla.draftapi.auth.UserInfo
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{NewAgreement, NewComment, NotFoundException, UpdatedComment}
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.language.Language.{AllLanguages, UnknownLanguage, findByLanguageOrBestEffort, mergeLanguageFields}
import no.ndla.mapping.License.getLicense
import no.ndla.validation._
import org.jsoup.nodes.Element
import scalikejdbc.{DBSession, ReadOnlyAutoSession}

import java.time.LocalDateTime
import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock
    with DraftRepository
    with ArticleApiClient
    with StateTransitionRules
    with WriteService
    with UUIDUtil
    with Props =>
  val converterService: ConverterService

  class ConverterService extends StrictLogging {
    import props.externalApiUrls

    def toDomainArticle(
        newArticleId: Long,
        newArticle: api.NewArticle,
        externalIds: List[String],
        user: UserInfo,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime]
    ): Try[Draft] = {
      val domainTitles = Seq(common.Title(newArticle.title, newArticle.language))
      val domainContent = newArticle.content
        .map(content => common.ArticleContent(removeUnknownEmbedTagAttributes(content), newArticle.language))
        .toSeq

      val status = externalIds match {
        case Nil => common.Status(PLANNED, Set.empty)
        case _   => common.Status(PLANNED, Set(IMPORTED))
      }

      val newAvailability = common.Availability.valueOf(newArticle.availability).getOrElse(common.Availability.everyone)
      val revisionMeta = newArticle.revisionMeta match {
        case Some(revs) if revs.nonEmpty =>
          newArticle.revisionMeta.map(_.map(toDomainRevisionMeta)).getOrElse(common.draft.RevisionMeta.default)
        case _ => common.draft.RevisionMeta.default
      }

      val responsible = newArticle.responsibleId.map(responsibleId =>
        Responsible(responsibleId = responsibleId, lastUpdated = clock.now())
      )

      newNotes(newArticle.notes, user, status).map(notes =>
        Draft(
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
          articleType = common.ArticleType.valueOfOrError(newArticle.articleType),
          notes = notes,
          previousVersionsNotes = Seq.empty,
          editorLabels = newArticle.editorLabels,
          grepCodes = newArticle.grepCodes,
          conceptIds = newArticle.conceptIds,
          availability = newAvailability,
          relatedContent = toDomainRelatedContent(newArticle.relatedContent),
          revisionMeta = revisionMeta,
          responsible = responsible,
          slug = newArticle.slug,
          comments = newCommentToDomain(newArticle.comments)
        )
      )
    }

    private[service] def updatedCommentToDomain(
        updatedComments: List[UpdatedComment],
        existingComments: Seq[Comment]
    ): Seq[Comment] = {
      updatedComments.map(updatedComment => {
        existingComments.find(cc => updatedComment.id.contains(cc.id.toString)) match {
          case Some(existingComment) =>
            val isContentChanged = updatedComment.content != existingComment.content
            val newUpdated       = if (isContentChanged) clock.now() else existingComment.updated
            existingComment.copy(
              updated = newUpdated,
              content = updatedComment.content
            )
          case None =>
            Comment(
              id = uuidUtil.randomUUID(),
              created = clock.now(),
              updated = clock.now(),
              content = updatedComment.content
            )
        }
      })
    }

    private[service] def newCommentToDomain(newComment: List[NewComment]): Seq[Comment] = {
      newComment.map(comment =>
        Comment(
          id = UUID.randomUUID(),
          created = clock.now(),
          updated = clock.now(),
          content = comment.content
        )
      )
    }

    private[service] def updatedCommentToDomainNullDocument(
        updatedComments: List[UpdatedComment]
    ): Try[Seq[Comment]] = {
      updatedComments.traverse(comment =>
        comment.id match {
          case Some(uuid) =>
            Try(UUID.fromString(uuid)).map(uuid =>
              Comment(
                id = uuid,
                created = clock.now(),
                updated = clock.now(),
                content = comment.content
              )
            )
          case None =>
            Success(
              Comment(
                id = uuidUtil.randomUUID(),
                created = clock.now(),
                updated = clock.now(),
                content = comment.content
              )
            )
        }
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

    def toDomainRelatedContent(relatedContent: Seq[commonApi.RelatedContent]): Seq[common.RelatedContent] = {
      relatedContent.map {
        case Left(x)  => Left(RelatedContentLink(url = x.url, title = x.title))
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

    def toDomainTitle(articleTitle: api.ArticleTitle): common.Title =
      common.Title(articleTitle.title, articleTitle.language)

    def toDomainContent(articleContent: api.ArticleContent): common.ArticleContent = {
      common.ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.language)
    }

    def toDomainTag(tag: api.ArticleTag): common.Tag = common.Tag(tag.tags, tag.language)

    def toDomainTag(tag: Seq[String], language: String): Option[common.Tag] =
      if (tag.nonEmpty) Some(common.Tag(tag, language)) else None

    def toDomainVisualElement(visual: api.VisualElement): common.VisualElement = {
      common.VisualElement(removeUnknownEmbedTagAttributes(visual.visualElement), visual.language)
    }

    def toDomainVisualElement(visual: String, language: String): common.VisualElement =
      common.VisualElement(removeUnknownEmbedTagAttributes(visual), language)

    def toDomainIntroduction(intro: api.ArticleIntroduction): common.Introduction = {
      common.Introduction(intro.introduction, intro.language)
    }

    def toDomainIntroduction(intro: String, language: String): common.Introduction =
      common.Introduction(intro, language)

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): common.Description = {
      common.Description(meta.metaDescription, meta.language)
    }

    def toDomainMetaDescription(meta: String, language: String): common.Description =
      common.Description(meta, language)

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

    def getEmbeddedConceptIds(article: Draft): Seq[Long] = {
      val htmlElements = article.content.map(content => HtmlTagRules.stringToJsoupDocument(content.content))
      val conceptEmbeds = htmlElements.flatMap(elem => {
        val conceptSelector = s"$EmbedTagName[${TagAttributes.DataResource}=${ResourceType.Concept}]"
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

    def getEmbeddedH5PPaths(article: Draft): Seq[String] = {
      val getH5PEmbeds = (htmlElements: Seq[Element]) => {
        htmlElements.flatMap(elem => {
          val h5pSelector = s"$EmbedTagName[${TagAttributes.DataResource}=${ResourceType.H5P}]"
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

    private def getLinkToOldNdla(id: Long)(implicit session: DBSession): Option[String] =
      draftRepository.getExternalIdsFromId(id).map(createLinkToOldNdla).headOption

    private def removeUnknownEmbedTagAttributes(html: String): String = {
      val document = HtmlTagRules.stringToJsoupDocument(html)
      document
        .select(EmbedTagName)
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
        status: DraftStatus.Value,
        draft: Draft,
        user: UserInfo,
        isImported: Boolean
    ): IO[Try[Draft]] = StateTransitionRules.doTransition(draft, status, user, isImported)

    def toApiResponsible(responsible: Responsible): api.DraftResponsible =
      api.DraftResponsible(
        responsibleId = responsible.responsibleId,
        lastUpdated = responsible.lastUpdated
      )

    def toApiArticle(article: Draft, language: String, fallback: Boolean = false): Try[api.Article] = {
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
        val responsible    = article.responsible.map(toApiResponsible)

        Success(
          api.Article(
            id = article.id.get,
            oldNdlaUrl = article.id.flatMap(id => getLinkToOldNdla(id)(ReadOnlyAutoSession)),
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
            revisions = revisionMetas,
            responsible = responsible,
            slug = article.slug,
            comments = article.comments.map(toApiComment).sortBy(_.created).reverse
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

    def toApiArticleTitle(title: common.Title): api.ArticleTitle = api.ArticleTitle(title.title, title.language)

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

    def toApiRelatedContent(relatedContent: common.RelatedContent): commonApi.RelatedContent = {
      relatedContent match {
        case Left(x)  => Left(commonApi.RelatedContentLink(url = x.url, title = x.title))
        case Right(x) => Right(x)
      }
    }

    def toApiComment(comment: Comment): api.Comment = api.Comment(
      id = comment.id.toString,
      content = comment.content,
      created = comment.created,
      updated = comment.updated
    )

    def toApiArticleTag(tag: common.Tag): api.ArticleTag = api.ArticleTag(tag.tags, tag.language)

    def toApiRequiredLibrary(required: common.RequiredLibrary): api.RequiredLibrary = {
      api.RequiredLibrary(required.mediaType, required.name, required.url)
    }

    def toApiVisualElement(visual: common.VisualElement): api.VisualElement =
      api.VisualElement(visual.resource, visual.language)

    def toApiArticleIntroduction(intro: common.Introduction): api.ArticleIntroduction = {
      api.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticleMetaDescription(metaDescription: common.Description): api.ArticleMetaDescription = {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def toArticleApiCopyright(copyright: common.draft.Copyright): common.article.Copyright = {
      common.article.Copyright(
        copyright.license.getOrElse(""),
        copyright.origin.getOrElse(""),
        copyright.creators,
        copyright.processors,
        copyright.rightsholders,
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def deleteLanguage(
        article: Draft,
        language: String,
        userInfo: UserInfo
    ): Try[Draft] = {
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

    def getNextRevision(draft: Draft): Option[common.draft.RevisionMeta] = getNextRevision(draft.revisionMeta)
    def getNextRevision(revisions: Seq[common.draft.RevisionMeta]): Option[common.draft.RevisionMeta] =
      revisions.filterNot(_.status == common.draft.RevisionStatus.Revised).sortBy(_.revisionDate).headOption

    def toArticleApiArticle(draft: Draft): Try[common.article.Article] = {
      draft.copyright match {
        case None => Failure(ValidationException("copyright", "Copyright must be present when publishing an article"))
        case Some(copyright) =>
          Success(
            common.article.Article(
              id = draft.id,
              revision = draft.revision,
              title = draft.title,
              content = draft.content,
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
              revisionDate = getNextRevision(draft.revisionMeta).map(_.revisionDate),
              slug = draft.slug
            )
          )
      }
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
      val fileEmbeds = doc.select(s"$EmbedTagName[${TagAttributes.DataResource}='${ResourceType.File}']").asScala.toSeq
      fileEmbeds.flatMap(e => Option(e.attr(TagAttributes.DataPath.toString)))
    }

    def cloneFilesIfExists(existingContent: Seq[String], newContent: String): Try[String] = {
      val existingFiles = existingContent.flatMap(getExistingPaths)

      val doc        = HtmlTagRules.stringToJsoupDocument(newContent)
      val fileEmbeds = doc.select(s"$EmbedTagName[${TagAttributes.DataResource}='${ResourceType.File}']").asScala

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
        toMergeInto: Draft
    ): Try[Seq[common.EditorNote]] = {
      val newLanguageEditorNote =
        if (isNewLanguage) Seq(s"Ny språkvariant '${article.language.getOrElse("und")}' ble lagt til.")
        else Seq.empty

      val changedResponsible =
        article.responsibleId match {
          case Right(Some(newId)) if !toMergeInto.responsible.map(_.responsibleId).contains(newId) =>
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

    def toDomainArticle(
        toMergeInto: Draft,
        article: api.UpdatedArticle,
        isImported: Boolean,
        user: UserInfo,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime]
    ): Try[Draft] = {
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

      val responsible = (article.responsibleId, toMergeInto.responsible) match {
        case (Left(_), _)                       => None
        case (Right(Some(responsibleId)), None) => Some(Responsible(responsibleId, clock.now()))
        case (Right(Some(responsibleId)), Some(existing)) if existing.responsibleId != responsibleId =>
          Some(Responsible(responsibleId, clock.now()))
        case (Right(_), existing) => existing
      }

      val updatedComments = article.comments
        .map(comments => updatedCommentToDomain(comments, toMergeInto.comments))
        .getOrElse(toMergeInto.comments)

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
            articleType = article.articleType.map(common.ArticleType.valueOfOrError).getOrElse(toMergeInto.articleType),
            notes = allNotes,
            editorLabels = article.editorLabels.getOrElse(toMergeInto.editorLabels),
            grepCodes = article.grepCodes.getOrElse(toMergeInto.grepCodes),
            conceptIds = article.conceptIds.getOrElse(toMergeInto.conceptIds),
            availability = updatedAvailability,
            relatedContent = updatedRelatedContent,
            revisionMeta = updatedRevisionMeta,
            responsible = responsible,
            slug = article.slug.orElse(toMergeInto.slug),
            comments = updatedComments
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
        toMergeInto: Draft,
        updatedArticle: api.UpdatedArticle,
        lang: String
    ): Draft = {
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
    ): Try[Draft] = {
      val createdDate = oldNdlaCreatedDate.getOrElse(clock.now())
      val updatedDate = oldNdlaUpdatedDate.getOrElse(clock.now())

      article.language match {
        case None =>
          val error = ValidationMessage("language", "This field must be specified when updating language fields")
          Failure(new ValidationException(errors = Seq(error)))
        case Some(lang) =>
          val status =
            if (isImported) common.Status(PLANNED, Set(IMPORTED))
            else common.Status(PLANNED, Set.empty)

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

          val responsible = article.responsibleId
            .getOrElse(None)
            .map(responsibleId => Responsible(responsibleId = responsibleId, lastUpdated = clock.now()))

          for {
            comments <- updatedCommentToDomainNullDocument(article.comments.getOrElse(List.empty))
            notes    <- mergedNotes
          } yield Draft(
            id = Some(id),
            revision = Some(1),
            status = status,
            title = article.title.map(t => common.Title(t, lang)).toSeq,
            content = article.content.map(c => common.ArticleContent(c, lang)).toSeq,
            copyright = article.copyright.map(toDomainCopyright),
            tags = article.tags.toSeq.map(tags => common.Tag(tags, lang)),
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
              .map(common.ArticleType.valueOfOrError)
              .getOrElse(common.ArticleType.Standard),
            notes = notes,
            previousVersionsNotes = Seq.empty,
            editorLabels = article.editorLabels.getOrElse(Seq.empty),
            grepCodes = article.grepCodes.getOrElse(Seq.empty),
            conceptIds = article.conceptIds.getOrElse(Seq.empty),
            availability = updatedAvailability,
            relatedContent = article.relatedContent.map(toDomainRelatedContent).getOrElse(Seq.empty),
            revisionMeta = updatedRevisionMeta,
            responsible = responsible,
            slug = article.slug,
            comments = comments
          )
      }
    }

    private[service] def _stateTransitionsToApi(
        user: UserInfo,
        article: Option[Draft]
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

    def addNote(article: Draft, noteText: String, user: UserInfo): Draft = {
      article.copy(
        notes = article.notes :+ common.EditorNote(noteText, user.id, article.status, clock.now())
      )
    }

  }

}

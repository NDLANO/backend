/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.Path
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.Clock
import no.ndla.common.ContentURIUtil.parseArticleIdAndRevision
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationException
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.model.api.UpdateWith
import no.ndla.common.model.domain.{Priority, Responsible, UploadedFile}
import no.ndla.common.model.domain.draft.DraftStatus.{IN_PROGRESS, PLANNED, PUBLISHED}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration.*
import no.ndla.draftapi.model.api.PartialArticleFieldsDTO
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.search.{ArticleIndexService, GrepCodesIndexService, TagIndexService}
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.language.Language
import no.ndla.language.Language.UnknownLanguage
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.*
import org.jsoup.nodes.Element
import scalikejdbc.{AutoSession, ReadOnlyAutoSession}

import java.util.concurrent.Executors
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.jdk.CollectionConverters.*
import scala.math.max
import scala.util.{Failure, Random, Success, Try}

trait WriteService {
  this: DraftRepository & UserDataRepository & ConverterService & ContentValidator & ArticleIndexService &
    TagIndexService & GrepCodesIndexService & Clock & ReadService & ArticleApiClient & SearchApiClient &
    FileStorageService & TaxonomyApiClient & Props =>
  val writeService: WriteService

  class WriteService extends StrictLogging {

    def insertDump(article: Draft): Try[Draft] =
      draftRepository.rollbackOnFailure(implicit session => {
        draftRepository
          .newEmptyArticleId()
          .map(newId => {
            val artWithId = article.copy(id = Some(newId))
            draftRepository.insert(artWithId)
          })
      })

    private def indexArticle(article: Draft, user: TokenUser): Try[Unit] = {
      val executor = Executors.newSingleThreadExecutor
      val ec       = ExecutionContext.fromExecutorService(executor)

      article.id match {
        case None => Failure(new IllegalStateException("No id found for article when indexing. This is a bug."))
        case Some(articleId) =>
          searchApiClient.indexDraft(article, user)(ec): Unit
          articleIndexService.indexAsync(articleId, article)(ec): Unit
          tagIndexService.indexAsync(articleId, article)(ec): Unit
          grepCodesIndexService.indexAsync(articleId, article)(ec): Unit
          Success(())
      }

    }

    def copyArticleFromId(
        articleId: Long,
        userInfo: TokenUser,
        language: String,
        fallback: Boolean,
        usePostFix: Boolean
    ): Try[api.ArticleDTO] = {
      draftRepository.rollbackOnFailure { implicit session =>
        draftRepository.withId(articleId) match {
          case None => Failure(api.NotFoundException(s"Article with id '$articleId' was not found in database."))
          case Some(article) =>
            for {
              newId <- draftRepository.newEmptyArticleId()
              status = common.Status(PLANNED, Set.empty)
              notes <- converterService.newNotes(
                Seq(s"Opprettet artikkel, som kopi av artikkel med id: '$articleId'."),
                userInfo,
                status
              )
              newTitles = if (usePostFix) article.title.map(t => t.copy(title = t.title + " (Kopi)")) else article.title
              newContents <- contentWithClonedFiles(article.content.toList)
              newResponsible = Some(Responsible(userInfo.id, clock.now()))
              articleToInsert = article.copy(
                id = Some(newId),
                title = newTitles,
                content = newContents,
                revision = Some(1),
                updated = clock.now(),
                created = clock.now(),
                published = clock.now(),
                updatedBy = userInfo.id,
                responsible = newResponsible,
                status = status,
                notes = notes
              )
              inserted = draftRepository.insert(articleToInsert)
              _        = indexArticle(inserted, userInfo)
              enriched = readService.addUrlsOnEmbedResources(inserted)
              converted <- converterService.toApiArticle(enriched, language, fallback)
            } yield converted
        }
      }
    }

    def contentWithClonedFiles(contents: List[common.ArticleContent]): Try[List[common.ArticleContent]] = {
      contents.traverse(content => {
        val doc    = HtmlTagRules.stringToJsoupDocument(content.content)
        val embeds = doc.select(s"$EmbedTagName[${TagAttribute.DataResource}='${ResourceType.File}']").asScala

        embeds.toList.traverse(cloneEmbedAndUpdateElement) match {
          case Failure(ex) => Failure(ex)
          case Success(_)  => Success(content.copy(HtmlTagRules.jsoupDocumentToString(doc)))
        }
      })
    }

    /** MUTATES fileEmbed by cloning file and updating data-path */
    def cloneEmbedAndUpdateElement(fileEmbed: Element): Try[Element] = {
      Option(fileEmbed.attr(TagAttribute.DataPath.toString)) match {
        case Some(existingPath) =>
          cloneFileAndGetNewPath(existingPath).map(newPath => {
            // Jsoup is mutable and we use it here to update the embeds data-path with the cloned file
            fileEmbed.attr(TagAttribute.DataPath.toString, newPath)
          })
        case None =>
          Failure(api.CloneFileException(s"Could not get ${TagAttribute.DataPath} of file embed '$fileEmbed'."))
      }
    }

    private def cloneFileAndGetNewPath(oldPath: String): Try[String] = {
      val ext           = getFileExtension(oldPath).getOrElse("")
      val newFileName   = randomFilename(ext)
      val withoutPrefix = Path.parse(oldPath).parts.dropWhile(_ == "files").mkString("/")
      fileStorage.copyResource(withoutPrefix, newFileName).map(f => s"/files/$f")
    }

    def newArticle(
        newArticle: api.NewArticleDTO,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        user: TokenUser,
        oldNdlaCreatedDate: Option[NDLADate],
        oldNdlaUpdatedDate: Option[NDLADate],
        importId: Option[String]
    ): Try[api.ArticleDTO] = {
      val newNotes      = Some("Opprettet artikkel" +: newArticle.notes.getOrElse(Seq.empty))
      val visualElement = newArticle.visualElement.filter(_.nonEmpty)
      val withNotes = newArticle.copy(
        notes = newNotes,
        visualElement = visualElement
      )
      draftRepository.rollbackOnFailure { implicit session =>
        val insertFunction = externalIds match {
          case Nil =>
            (a: Draft) => draftRepository.insert(a)
          case nids =>
            (a: Draft) => draftRepository.insertWithExternalIds(a, nids, externalSubjectIds, importId)
        }

        for {
          newId <- draftRepository.newEmptyArticleId()
          domainArticle <- converterService.toDomainArticle(
            newId,
            withNotes,
            externalIds,
            user,
            oldNdlaCreatedDate,
            oldNdlaUpdatedDate
          )
          _               <- contentValidator.validateArticle(None, domainArticle)
          insertedArticle <- Try(insertFunction(domainArticle))
          _ = indexArticle(insertedArticle, user)
          apiArticle <- converterService.toApiArticle(insertedArticle, newArticle.language)
        } yield apiArticle
      }
    }

    def updateArticleStatus(
        status: DraftStatus,
        id: Long,
        user: TokenUser,
        isImported: Boolean
    ): Try[api.ArticleDTO] = {
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case None => Failure(api.NotFoundException(s"No article with id $id was found"))
        case Some(draft) =>
          for {
            convertedArticle <- converterService.updateStatus(status, draft, user, isImported)
            updatedArticle <- updateArticleAndStoreAsNewIfPublished(
              convertedArticle,
              isImported,
              statusWasUpdated = true
            )
            _ = indexArticle(updatedArticle, user)
            apiArticle <- converterService.toApiArticle(updatedArticle, Language.AllLanguages, fallback = true)
          } yield apiArticle
      }
    }

    private def updateArticleAndStoreAsNewIfPublished(
        article: Draft,
        isImported: Boolean,
        statusWasUpdated: Boolean
    ): Try[Draft] = {
      val storeAsNewVersion = statusWasUpdated && article.status.current == PUBLISHED && !isImported
      draftRepository.rollbackOnFailure { implicit session =>
        draftRepository.updateArticle(article, isImported) match {
          case Success(updated) if storeAsNewVersion => draftRepository.storeArticleAsNewVersion(updated, None)
          case Success(updated)                      => Success(updated)
          case Failure(ex)                           => Failure(ex)
        }
      }
    }

    private def updateArticleWithExternalAndStoreAsNewIfPublished(
        article: Draft,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        importId: Option[String],
        statusWasUpdated: Boolean
    ): Try[Draft] = {
      val storeAsNewVersion = statusWasUpdated && article.status.current == PUBLISHED
      draftRepository.rollbackOnFailure { implicit session =>
        draftRepository.updateWithExternalIds(article, externalIds, externalSubjectIds, importId) match {
          case Success(updated) if storeAsNewVersion => draftRepository.storeArticleAsNewVersion(updated, None)
          case Success(updated)                      => Success(updated)
          case Failure(ex)                           => Failure(ex)
        }
      }
    }

    /** Determines which repository function(s) should be called and calls them */
    private def performArticleUpdate(
        article: Draft,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        isImported: Boolean,
        importId: Option[String],
        createNewVersion: Boolean,
        user: TokenUser,
        statusWasUpdated: Boolean
    ): Try[Draft] =
      if (createNewVersion) {
        draftRepository.storeArticleAsNewVersion(article, Some(user), keepDraftData = true)(AutoSession)
      } else {
        externalIds match {
          case Nil => updateArticleAndStoreAsNewIfPublished(article, isImported, statusWasUpdated)
          case nids =>
            updateArticleWithExternalAndStoreAsNewIfPublished(
              article,
              nids,
              externalSubjectIds,
              importId,
              statusWasUpdated
            )
        }
      }

    private def addRevisionDateNotes(
        user: TokenUser,
        updatedArticle: Draft,
        oldArticle: Option[Draft]
    ): Draft = {
      val oldRevisions = oldArticle.map(a => a.revisionMeta).getOrElse(Seq.empty)
      val oldIds       = oldRevisions.map(rm => rm.id).toSet
      val newIds       = updatedArticle.revisionMeta.map(rm => rm.id).toSet
      val deleted = oldRevisions
        .filterNot(old => newIds.contains(old.id))
        .map(del => common.EditorNote(s"Slettet revisjon ${del.note}.", user.id, updatedArticle.status, clock.now()))

      val notes = updatedArticle.revisionMeta.flatMap {
        case rm if !oldIds.contains(rm.id) && rm.status == common.draft.RevisionStatus.Revised =>
          common
            .EditorNote(
              s"Lagt til og fullført revisjon ${rm.note}.",
              user.id,
              updatedArticle.status,
              clock.now()
            )
            .some
        case rm if !oldIds.contains(rm.id) =>
          common.EditorNote(s"Lagt til revisjon ${rm.note}.", user.id, updatedArticle.status, clock.now()).some
        case rm =>
          oldRevisions.find(_.id == rm.id) match {
            case Some(old) if old.status != rm.status && rm.status == common.draft.RevisionStatus.Revised =>
              common
                .EditorNote(s"Fullført revisjon ${rm.note}.", user.id, updatedArticle.status, clock.now())
                .some
            case Some(old) if old != rm =>
              common
                .EditorNote(s"Endret revisjon ${rm.note}.", user.id, updatedArticle.status, clock.now())
                .some
            case _ => None
          }
      }

      updatedArticle.copy(notes = updatedArticle.notes ++ notes ++ deleted)
    }

    private def hasResponsibleBeenUpdated(
        draft: Draft,
        oldDraft: Option[Draft]
    ): Boolean = {
      draft.responsible match {
        case None => false
        case Some(responsible) =>
          val oldResponsibleId  = oldDraft.flatMap(_.responsible).map(_.responsibleId)
          val hasNewResponsible = !oldResponsibleId.contains(responsible.responsibleId)
          hasNewResponsible
      }
    }

    private def updateStartedField(
        draft: Draft,
        oldDraft: Option[Draft],
        statusWasUpdated: Boolean,
        updatedApiArticle: api.UpdatedArticleDTO,
        shouldNotAutoUpdateStatus: Boolean
    ): Draft = {
      val isAutomaticResponsibleChange = updatedApiArticle.responsibleId match {
        case UpdateWith(_) => false
        case _             => true
      }

      val isAutomaticStatusChange     = updatedApiArticle.status.isEmpty
      val isAutomaticOnEditTransition = isAutomaticResponsibleChange && isAutomaticStatusChange

      if (shouldNotAutoUpdateStatus && draft.status.current == PUBLISHED) {
        draft.copy(started = false)
      } else if (shouldNotAutoUpdateStatus) {
        draft
      } else if (isAutomaticOnEditTransition && statusWasUpdated) {
        draft.copy(started = true)
      } else {
        val responsibleIdWasUpdated = hasResponsibleBeenUpdated(draft, oldDraft)

        val shouldReset = statusWasUpdated && !isAutomaticStatusChange || responsibleIdWasUpdated
        draft.copy(started = !shouldReset)
      }
    }

    private def updatePriorityField(
        draft: Draft,
        oldDraft: Option[Draft],
        statusWasUpdated: Boolean
    ): Draft = {
      if (draft.priority == Priority.OnHold) {
        val responsibleIdWasUpdated = hasResponsibleBeenUpdated(draft, oldDraft)
        if (responsibleIdWasUpdated || statusWasUpdated) {
          draft.copy(priority = Priority.Unspecified)
        } else draft
      } else draft

    }

    private def addPartialPublishNote(
        draft: Draft,
        user: TokenUser,
        partialPublishFields: Set[PartialArticleFieldsDTO]
    ): Draft =
      if (partialPublishFields.nonEmpty)
        converterService.addNote(draft, "Artikkelen har blitt delpublisert", user)
      else draft

    private def updateArticle(
        toUpdate: Draft,
        importId: Option[String],
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        language: Option[String],
        isImported: Boolean,
        createNewVersion: Boolean,
        oldArticle: Option[Draft],
        user: TokenUser,
        statusWasUpdated: Boolean,
        updatedApiArticle: api.UpdatedArticleDTO,
        shouldNotAutoUpdateStatus: Boolean
    ): Try[Draft] = {
      val fieldsToPartialPublish = shouldPartialPublish(oldArticle, toUpdate)
      val withPartialPublishNote = addPartialPublishNote(toUpdate, user, fieldsToPartialPublish)
      val withRevisionDateNotes  = addRevisionDateNotes(user, withPartialPublishNote, oldArticle)
      val withStarted = updateStartedField(
        withRevisionDateNotes,
        oldArticle,
        statusWasUpdated,
        updatedApiArticle,
        shouldNotAutoUpdateStatus
      )
      val withPriority =
        updatePriorityField(withStarted, oldArticle, statusWasUpdated)

      for {
        _ <- contentValidator.validateArticleOnLanguage(oldArticle, toUpdate, language)
        domainArticle <- performArticleUpdate(
          withPriority,
          externalIds,
          externalSubjectIds,
          isImported,
          importId,
          createNewVersion,
          user,
          statusWasUpdated
        )
        _ = partialPublishIfNeeded(
          domainArticle,
          fieldsToPartialPublish.toSeq,
          language.getOrElse(Language.AllLanguages),
          user
        )
        _ = indexArticle(domainArticle, user)
        _ <- updateTaxonomyForArticle(domainArticle, user)
      } yield domainArticle
    }

    private def updateTaxonomyForArticle(article: Draft, user: TokenUser) = {
      article.id match {
        case Some(id) => taxonomyApiClient.updateTaxonomyIfExists(id, article, user).map(_ => article)
        case None =>
          Failure(
            api.ArticleVersioningException("Article supplied to taxonomy update did not have an id. This is a bug.")
          )
      }
    }

    def shouldUpdateStatus(changedArticle: Draft, existingArticle: Draft): Boolean = {
      // Function that sets values we don't want to include when comparing articles to check if we should update status
      val withComparableValues =
        (article: Draft) =>
          converterService
            .withSortedLanguageFields(article)
            .copy(
              revision = None,
              notes = Seq.empty,
              editorLabels = Seq.empty,
              created = NDLADate.MIN,
              updated = NDLADate.MIN,
              published = NDLADate.MIN,
              updatedBy = "",
              availability = common.Availability.everyone,
              grepCodes = Seq.empty,
              copyright = article.copyright.map(e => e.copy(license = None)),
              metaDescription = Seq.empty,
              relatedContent = Seq.empty,
              tags = Seq.empty,
              revisionMeta = Seq.empty,
              comments = List.empty,
              priority = Priority.Unspecified,
              started = false,
              qualityEvaluation = None
            )

      val comparableNew      = withComparableValues(changedArticle)
      val comparableExisting = withComparableValues(existingArticle)
      val shouldUpdateStatus = comparableNew != comparableExisting
      shouldUpdateStatus
    }

    /** Compares articles to check whether earliest not-revised revision date has changed since that is the only one
      * article-api cares about.
      */
    private def compareRevisionDates(oldArticle: Draft, newArticle: Draft): Boolean = {
      converterService.getNextRevision(oldArticle) != converterService.getNextRevision(newArticle)
    }

    private def compareField(
        field: api.PartialArticleFieldsDTO,
        old: Draft,
        changed: Draft
    ): Option[api.PartialArticleFieldsDTO] = {
      import api.PartialArticleFieldsDTO.*
      val shouldInclude = field match {
        case `availability`    => old.availability != changed.availability
        case `grepCodes`       => old.grepCodes != changed.grepCodes
        case `relatedContent`  => old.relatedContent != changed.relatedContent
        case `tags`            => old.tags.sorted != changed.tags.sorted
        case `metaDescription` => old.metaDescription.sorted != changed.metaDescription.sorted
        case `license`         => old.copyright.flatMap(_.license) != changed.copyright.flatMap(_.license)
        case `revisionDate`    => compareRevisionDates(old, changed)
        case `published`       => old.published != changed.published
      }

      Option.when(shouldInclude)(field)
    }

    /** Returns fields to publish _if_ partial-publishing requirements are satisfied, otherwise returns empty set. */
    private[service] def shouldPartialPublish(
        existingArticle: Option[Draft],
        changedArticle: Draft
    ): Set[api.PartialArticleFieldsDTO] = {
      val isPublished =
        changedArticle.status.current == PUBLISHED ||
          changedArticle.status.other.contains(PUBLISHED)

      if (isPublished) {
        val changedFields = existingArticle
          .map(e => api.PartialArticleFieldsDTO.values.flatMap(field => compareField(field, e, changedArticle)))
          .getOrElse(api.PartialArticleFieldsDTO.values)

        changedFields.toSet
      } else {
        Set.empty
      }
    }

    private[service] def updateStatusIfNeeded(
        convertedArticle: Draft,
        existingArticle: Draft,
        updatedApiArticle: api.UpdatedArticleDTO,
        user: TokenUser,
        shouldNotAutoUpdateStatus: Boolean
    ): Try[Draft] = {
      val newManualStatus = updatedApiArticle.status.traverse(DraftStatus.valueOfOrError).?
      if (shouldNotAutoUpdateStatus && newManualStatus.isEmpty)
        return Success(convertedArticle)

      val oldStatus            = existingArticle.status.current
      val newStatusIfUndefined = if (oldStatus == PUBLISHED) IN_PROGRESS else oldStatus
      val newStatus            = newManualStatus.getOrElse(newStatusIfUndefined)

      converterService.updateStatus(newStatus, convertedArticle, user, isImported = false)
    }

    def updateArticle(
        articleId: Long,
        updatedApiArticle: api.UpdatedArticleDTO,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        user: TokenUser,
        oldNdlaCreatedDate: Option[NDLADate],
        oldNdlaUpdatedDate: Option[NDLADate],
        importId: Option[String]
    ): Try[api.ArticleDTO] = {
      draftRepository.withId(articleId)(ReadOnlyAutoSession) match {
        case Some(existing) =>
          updateExistingArticle(
            existing,
            updatedApiArticle,
            externalIds,
            externalSubjectIds,
            user,
            oldNdlaCreatedDate,
            oldNdlaUpdatedDate,
            importId
          )
        case None =>
          Failure(api.NotFoundException(s"Article with id $articleId does not exist"))
      }
    }

    private def updateExistingArticle(
        existing: Draft,
        updatedApiArticle: api.UpdatedArticleDTO,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        user: TokenUser,
        oldNdlaCreatedDate: Option[NDLADate],
        oldNdlaUpdatedDate: Option[NDLADate],
        importId: Option[String]
    ): Try[api.ArticleDTO] = for {
      convertedArticle <- converterService.toDomainArticle(
        existing,
        updatedApiArticle,
        externalIds.nonEmpty,
        user,
        oldNdlaCreatedDate,
        oldNdlaUpdatedDate
      )

      shouldNotAutoUpdateStatus = !shouldUpdateStatus(convertedArticle, existing)

      articleWithStatus <- updateStatusIfNeeded(
        convertedArticle,
        existing,
        updatedApiArticle,
        user,
        shouldNotAutoUpdateStatus
      )

      didUpdateStatus = articleWithStatus.status.current != convertedArticle.status.current

      updatedArticle <- updateArticle(
        articleWithStatus,
        importId,
        externalIds,
        externalSubjectIds,
        language = updatedApiArticle.language,
        isImported = externalIds.nonEmpty,
        createNewVersion = updatedApiArticle.createNewVersion.getOrElse(false),
        oldArticle = Some(existing),
        user = user,
        statusWasUpdated = didUpdateStatus,
        updatedApiArticle = updatedApiArticle,
        shouldNotAutoUpdateStatus = shouldNotAutoUpdateStatus
      )
      withEmbedUrls = readService.addUrlsOnEmbedResources(updatedArticle)

      apiArticle <- converterService.toApiArticle(
        article = withEmbedUrls,
        language = updatedApiArticle.language.getOrElse(UnknownLanguage.toString),
        fallback = updatedApiArticle.language.isEmpty
      )
    } yield apiArticle

    def deleteLanguage(id: Long, language: String, userInfo: TokenUser): Try[api.ArticleDTO] = {
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case Some(article) =>
          article.title.size match {
            case 1 => Failure(api.OperationNotAllowedException("Only one language left"))
            case _ =>
              converterService
                .deleteLanguage(article, language, userInfo)
                .flatMap(newArticle =>
                  updateArticleAndStoreAsNewIfPublished(newArticle, isImported = false, statusWasUpdated = true)
                    .flatMap(
                      converterService.toApiArticle(_, Language.AllLanguages)
                    )
                )
          }
        case None => Failure(api.NotFoundException("Article does not exist"))
      }

    }

    def deleteArticle(id: Long): Try[api.ContentIdDTO] = {
      draftRepository
        .deleteArticle(id)(AutoSession)
        .flatMap(articleIndexService.deleteDocument)
        .map(id => api.ContentIdDTO(id))
    }

    def storeFile(file: UploadedFile): Try[api.UploadedFileDTO] =
      uploadFile(file).map(f =>
        api.UploadedFileDTO(
          filename = f.fileName,
          mime = f.contentType,
          extension = f.fileExtension,
          path = s"/files/${f.filePath}"
        )
      )

    private[service] def getFileExtension(fileName: String): Try[String] = {
      val badExtensionError =
        ValidationException(
          "file",
          s"The file must have one of the supported file extensions: '${props.supportedUploadExtensions.mkString(", ")}'"
        )

      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 =>
          props.supportedUploadExtensions.find(_ == fileName.substring(index).toLowerCase) match {
            case Some(e) => Success(e)
            case _       => Failure(badExtensionError)
          }
        case _ => Failure(badExtensionError)

      }
    }

    private[service] def getFilePathFromUrl(filePath: String) = {
      filePath.path.parts
        .dropWhile(_ == "files")
        .mkString("/")
    }

    def deleteFile(fileUrlOrPath: String): Try[?] = {
      val filePath = getFilePathFromUrl(fileUrlOrPath)
      if (fileStorage.resourceWithPathExists(filePath)) {
        fileStorage.deleteResourceWithPath(filePath)
      } else {
        Failure(api.NotFoundException(s"Could not find file with file path '$filePath' in storage."))
      }
    }

    private[service] def uploadFile(file: UploadedFile): Try[domain.UploadedFile] = {
      val fileExtension = file.fileName.flatMap(fn => getFileExtension(fn).toOption).getOrElse("")
      val contentType   = file.contentType.getOrElse("")
      val fileName      = LazyList.continually(randomFilename(fileExtension)).dropWhile(fileStorage.resourceExists).head

      fileStorage
        .uploadResourceFromStream(file, fileName)
        .map(_ =>
          domain.UploadedFile(
            fileName = fileName,
            filePath = s"${fileStorage.resourceDirectory}/$fileName",
            size = file.fileSize,
            contentType = contentType,
            fileExtension = fileExtension
          )
        )
    }

    private[service] def randomFilename(extension: String, length: Int = 20): String = {
      val extensionWithDot =
        if (!extension.headOption.contains('.') && extension.nonEmpty) s".$extension" else extension
      val randomString = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

    def newUserData(userId: String): Try[api.UserDataDTO] = {
      userDataRepository
        .insert(
          domain.UserData(
            id = None,
            userId = userId,
            savedSearches = None,
            latestEditedArticles = None,
            latestEditedConcepts = None,
            favoriteSubjects = None
          )
        )
        .map(converterService.toApiUserData)
    }

    def updateUserData(updatedUserData: api.UpdatedUserDataDTO, user: TokenUser): Try[api.UserDataDTO] = {
      val userId = user.id
      userDataRepository.withUserId(userId) match {
        case None =>
          val newUserData = domain.UserData(
            id = None,
            userId = userId,
            savedSearches = updatedUserData.savedSearches,
            latestEditedArticles = updatedUserData.latestEditedArticles,
            latestEditedConcepts = updatedUserData.latestEditedConcepts,
            favoriteSubjects = updatedUserData.favoriteSubjects
          )
          userDataRepository.insert(newUserData).map(converterService.toApiUserData)

        case Some(existing) =>
          val toUpdate = existing.copy(
            savedSearches = updatedUserData.savedSearches.orElse(existing.savedSearches),
            latestEditedArticles = updatedUserData.latestEditedArticles.orElse(existing.latestEditedArticles),
            latestEditedConcepts = updatedUserData.latestEditedConcepts.orElse(existing.latestEditedConcepts),
            favoriteSubjects = updatedUserData.favoriteSubjects.orElse(existing.favoriteSubjects)
          )
          userDataRepository.update(toUpdate).map(converterService.toApiUserData)
      }
    }

    private[service] def partialArticleFieldsUpdate(
        article: Draft,
        articleFieldsToUpdate: Seq[api.PartialArticleFieldsDTO],
        language: String
    ): PartialPublishArticle = {
      val isAllLanguage  = language == Language.AllLanguages
      val initialPartial = PartialPublishArticle.empty()

      import api.PartialArticleFieldsDTO.*
      articleFieldsToUpdate.distinct.foldLeft(initialPartial)((partial, field) => {
        field match {
          case `availability`                     => partial.withAvailability(article.availability)
          case `grepCodes`                        => partial.withGrepCodes(article.grepCodes)
          case `license`                          => partial.withLicense(article.copyright.flatMap(_.license))
          case `metaDescription` if isAllLanguage => partial.withMetaDescription(article.metaDescription)
          case `metaDescription`                  => partial.withMetaDescription(article.metaDescription, language)
          case `relatedContent`                   => partial.withRelatedContent(article.relatedContent)
          case `tags` if isAllLanguage            => partial.withTags(article.tags)
          case `tags`                             => partial.withTags(article.tags, language)
          case `revisionDate`                     => partial.withEarliestRevisionDate(article.revisionMeta)
          case `published`                        => partial.withPublished(article.published)
        }
      })
    }

    def partialPublishAndConvertToApiArticle(
        id: Long,
        fieldsToPublish: Seq[api.PartialArticleFieldsDTO],
        language: String,
        fallback: Boolean,
        user: TokenUser
    ): Try[api.ArticleDTO] =
      partialPublish(id, fieldsToPublish, language, user)._2.flatMap(article =>
        converterService.toApiArticle(article, language, fallback)
      )

    def partialPublish(
        id: Long,
        articleFieldsToUpdate: Seq[api.PartialArticleFieldsDTO],
        language: String,
        user: TokenUser
    ): (Long, Try[Draft]) =
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case None => id -> Failure(api.NotFoundException(s"Could not find draft with id of $id to partial publish"))
        case Some(article) =>
          partialPublish(article, articleFieldsToUpdate, language, user): Unit
          id -> Success(article)
      }

    private def partialPublishIfNeeded(
        article: Draft,
        articleFieldsToUpdate: Seq[api.PartialArticleFieldsDTO],
        language: String,
        user: TokenUser
    ): Future[Try[Draft]] = {
      if (articleFieldsToUpdate.nonEmpty)
        partialPublish(article, articleFieldsToUpdate, language, user)
      else
        Future.successful(Success(article))
    }

    private def partialPublish(
        article: Draft,
        fieldsToPublish: Seq[api.PartialArticleFieldsDTO],
        language: String,
        user: TokenUser
    ): Future[Try[Draft]] = {
      article.id match {
        case None =>
          Future.successful(
            Failure(new IllegalStateException(s"Article to partial publish did not have id. This is a bug."))
          )
        case Some(id) =>
          implicit val executionContext: ExecutionContextExecutorService =
            ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))
          val partialArticle = partialArticleFieldsUpdate(article, fieldsToPublish, language)
          val requestInfo    = RequestInfo.fromThreadContext()
          val fut = Future {
            requestInfo.setThreadContextRequestInfo()
            articleApiClient.partialPublishArticle(id, partialArticle, user)
          }

          val logError = (ex: Throwable) =>
            logger.error(s"Failed to partial publish article with id '$id', with error", ex)

          fut.onComplete {
            case Failure(ex)          => logError(ex)
            case Success(Failure(ex)) => logError(ex)
            case _                    => logger.info(s"Successfully partial published article with id '$id'")
          }

          fut.map(_.map(_ => article))
      }
    }

    def partialPublishMultiple(
        language: String,
        partialBulk: api.PartialBulkArticlesDTO,
        user: TokenUser
    ): Try[api.MultiPartialPublishResultDTO] = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))
      val requestInfo = RequestInfo.fromThreadContext()

      val futures = partialBulk.articleIds.map(id =>
        Future {
          requestInfo.setThreadContextRequestInfo()
          partialPublish(id, partialBulk.fields, language, user)
        }
      )

      val duration = partialBulk.articleIds.size.minutes // Max 1 minute PR article to partial publish for timeout
      val future   = Future.sequence(futures)
      Try(Await.result(future, duration)) match {

        case Failure(ex) =>
          logger.error("Awaiting for partial publishing future failed.", ex)
          Failure(ex)

        case Success(res) =>
          val successes = res.collect { case (id, Success(_)) => id }
          val failures = res.collect { case (id, Failure(ex)) =>
            logger.error(s"Partial publishing $id failed with ${ex.getMessage}", ex)
            api.PartialPublishFailureDTO(id, ex.getMessage)
          }

          Success(
            api.MultiPartialPublishResultDTO(
              successes = successes,
              failures = failures
            )
          )
      }
    }

    private def getRevisionMetaForUrn(node: Node): Seq[common.draft.RevisionMeta] = {
      node.contentUri match {
        case Some(contentUri) =>
          parseArticleIdAndRevision(contentUri) match {
            case (Success(articleId), _) =>
              draftRepository.withId(articleId)(ReadOnlyAutoSession) match {
                case Some(article) => article.revisionMeta
                case _             => Seq.empty
              }
            case _ => Seq.empty
          }
        case _ => Seq.empty
      }
    }

    def copyRevisionDates(publicId: String): Try[Unit] = {
      taxonomyApiClient.getNode(publicId) match {
        case Failure(_) => Failure(api.NotFoundException(s"No topics with id $publicId"))
        case Success(topic) =>
          val revisionMeta = getRevisionMetaForUrn(topic)
          if (revisionMeta.nonEmpty) {
            for {
              topics    <- taxonomyApiClient.getChildNodes(publicId)
              resources <- taxonomyApiClient.getChildResources(publicId)
              _         <- topics.traverse(setRevisions(_, revisionMeta))
              _         <- resources.traverse(setRevisions(_, revisionMeta))
            } yield ()
          } else Success(())
      }
    }

    private def setRevisions(entity: Node, revisions: Seq[common.draft.RevisionMeta]): Try[?] = {
      val updateResult = entity.contentUri match {
        case Some(contentUri) =>
          parseArticleIdAndRevision(contentUri) match {
            case (Success(articleId), _) => updateArticleWithRevisions(articleId, revisions)
            case _                       => Success(())
          }
        case _ => Success(())
      }
      updateResult.map(_ => {
        entity match {
          case Node(id, _, _, _) =>
            taxonomyApiClient
              .getChildResources(id)
              .flatMap(resources => resources.traverse(setRevisions(_, revisions)))
          case _ => Success(())
        }
      })
    }

    private def updateArticleWithRevisions(articleId: Long, revisions: Seq[common.draft.RevisionMeta]): Try[?] = {
      draftRepository
        .withId(articleId)(ReadOnlyAutoSession)
        .traverse(article => {
          val revisionMeta = article.revisionMeta ++ revisions
          val toUpdate     = article.copy(revisionMeta = revisionMeta.distinct)
          draftRepository.updateArticle(toUpdate)(AutoSession)
        })
    }
  }
}

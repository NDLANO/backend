/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.Path
import io.lemonlabs.uri.typesafe.dsl._
import no.ndla.common.Clock
import no.ndla.common.ContentURIUtil.parseArticleIdAndRevision
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationException
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.domain.draft.DraftStatus.{IN_PROGRESS, PLANNED, PUBLISHED}
import no.ndla.common.model.{domain => common}
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration._
import no.ndla.draftapi.model.api.PartialArticleFields
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.repository.{AgreementRepository, DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.search.{
  AgreementIndexService,
  ArticleIndexService,
  GrepCodesIndexService,
  TagIndexService
}
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.language.Language
import no.ndla.language.Language.UnknownLanguage
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation._
import org.jsoup.nodes.Element
import org.scalatra.servlet.FileItem
import scalikejdbc.{AutoSession, ReadOnlyAutoSession}

import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.jdk.CollectionConverters._
import scala.math.max
import scala.util.{Failure, Random, Success, Try}

trait WriteService {
  this: DraftRepository
    with AgreementRepository
    with UserDataRepository
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with TagIndexService
    with GrepCodesIndexService
    with AgreementIndexService
    with Clock
    with ReadService
    with ArticleApiClient
    with SearchApiClient
    with FileStorageService
    with TaxonomyApiClient
    with Props =>
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

    private def indexArticle(article: Draft): Try[Unit] = {
      val executor = Executors.newSingleThreadExecutor
      val ec       = ExecutionContext.fromExecutorService(executor)

      article.id match {
        case None => Failure(new IllegalStateException("No id found for article when indexing. This is a bug."))
        case Some(articleId) =>
          searchApiClient.indexDraft(article)(ec): Unit
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
    ): Try[api.Article] = {
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
              articleToInsert = article.copy(
                id = Some(newId),
                title = newTitles,
                content = newContents,
                revision = Some(1),
                updated = clock.now(),
                created = clock.now(),
                published = clock.now(),
                updatedBy = userInfo.id,
                status = status,
                notes = notes
              )
              inserted = draftRepository.insert(articleToInsert)
              _        = indexArticle(inserted)
              enriched = readService.addUrlsOnEmbedResources(inserted)
              converted <- converterService.toApiArticle(enriched, language, fallback)
            } yield converted
        }
      }
    }

    def contentWithClonedFiles(contents: List[common.ArticleContent]): Try[List[common.ArticleContent]] = {
      contents.toList.traverse(content => {
        val doc    = HtmlTagRules.stringToJsoupDocument(content.content)
        val embeds = doc.select(s"$EmbedTagName[${TagAttributes.DataResource}='${ResourceType.File}']").asScala

        embeds.toList.traverse(cloneEmbedAndUpdateElement) match {
          case Failure(ex) => Failure(ex)
          case Success(_)  => Success(content.copy(HtmlTagRules.jsoupDocumentToString(doc)))
        }
      })
    }

    /** MUTATES fileEmbed by cloning file and updating data-path */
    def cloneEmbedAndUpdateElement(fileEmbed: Element): Try[Element] = {
      Option(fileEmbed.attr(TagAttributes.DataPath.toString)) match {
        case Some(existingPath) =>
          cloneFileAndGetNewPath(existingPath).map(newPath => {
            // Jsoup is mutable and we use it here to update the embeds data-path with the cloned file
            fileEmbed.attr(TagAttributes.DataPath.toString, newPath)
          })
        case None =>
          Failure(api.CloneFileException(s"Could not get ${TagAttributes.DataPath} of file embed '$fileEmbed'."))
      }
    }

    def cloneFileAndGetNewPath(oldPath: String): Try[String] = {
      val ext           = getFileExtension(oldPath).getOrElse("")
      val newFileName   = randomFilename(ext)
      val withoutPrefix = Path.parse(oldPath).parts.dropWhile(_ == "files").mkString("/")
      fileStorage.copyResource(withoutPrefix, newFileName).map(f => s"/files/$f")
    }

    def updateAgreement(
        agreementId: Long,
        updatedAgreement: api.UpdatedAgreement,
        user: TokenUser
    ): Try[api.Agreement] = {
      agreementRepository.withId(agreementId) match {
        case None => Failure(api.NotFoundException(s"Agreement with id $agreementId does not exist"))
        case Some(existing) =>
          val toUpdate = existing.copy(
            title = updatedAgreement.title.getOrElse(existing.title),
            content = updatedAgreement.content.getOrElse(existing.content),
            copyright =
              updatedAgreement.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
            updated = clock.now(),
            updatedBy = user.id
          )

          val dateErrors = updatedAgreement.copyright
            .map(updatedCopyright => contentValidator.validateDates(updatedCopyright))
            .getOrElse(Seq.empty)

          for {
            _         <- contentValidator.validateAgreement(toUpdate, preExistingErrors = dateErrors)
            agreement <- agreementRepository.update(toUpdate)
            _         <- agreementIndexService.indexDocument(agreement)
          } yield converterService.toApiAgreement(agreement)
      }
    }

    def newAgreement(newAgreement: api.NewAgreement, user: TokenUser): Try[api.Agreement] = {
      val apiErrors = contentValidator.validateDates(newAgreement.copyright)

      val domainAgreement = converterService.toDomainAgreement(newAgreement, user)
      contentValidator.validateAgreement(domainAgreement, preExistingErrors = apiErrors) match {
        case Success(_) =>
          val agreement = agreementRepository.insert(domainAgreement)
          agreementIndexService.indexDocument(agreement): Unit
          Success(converterService.toApiAgreement(agreement))
        case Failure(exception) => Failure(exception)
      }
    }

    def newArticle(
        newArticle: api.NewArticle,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        user: TokenUser,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime],
        importId: Option[String]
    ): Try[api.Article] = {
      val newNotes      = "Opprettet artikkel." +: newArticle.notes
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
          _               <- contentValidator.validateArticle(domainArticle)
          insertedArticle <- Try(insertFunction(domainArticle))
          _ = indexArticle(insertedArticle)
          apiArticle <- converterService.toApiArticle(insertedArticle, newArticle.language)
        } yield apiArticle
      }
    }

    def updateArticleStatus(
        status: DraftStatus,
        id: Long,
        user: TokenUser,
        isImported: Boolean
    ): Try[api.Article] = {
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case None => Failure(api.NotFoundException(s"No article with id $id was found"))
        case Some(draft) =>
          for {
            convertedArticle <- converterService
              .updateStatus(status, draft, user, isImported)
              .attempt
              .unsafeRunSync()
              .toTry
              .flatten
            updatedArticle <- updateArticleAndStoreAsNewIfPublished(
              convertedArticle,
              isImported,
              statusWasUpdated = true
            )
            _ = indexArticle(updatedArticle)
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

    def addRevisionDateNotes(
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

    private def updateStartedField(
        draft: Draft,
        oldDraft: Option[Draft],
        statusWasUpdated: Boolean,
        updatedApiArticle: api.UpdatedArticle
    ): Draft = {
      val isAutomaticReponsibleChange = updatedApiArticle.responsibleId.map(_.isEmpty).getOrElse(true)
      val isAutomaticStatusChange     = updatedApiArticle.status.isEmpty
      val isAutomaticOnEditTransition = isAutomaticReponsibleChange && isAutomaticStatusChange

      if (isAutomaticOnEditTransition) {
        draft.copy(started = true)
      } else {

        val responsibleIdWasUpdated = draft.responsible match {
          case None => false
          case Some(responsible) =>
            val oldResponsibleId  = oldDraft.flatMap(_.responsible).map(_.responsibleId)
            val hasNewResponsible = !oldResponsibleId.contains(responsible.responsibleId)
            hasNewResponsible
        }

        val shouldReset = statusWasUpdated && !isAutomaticStatusChange || responsibleIdWasUpdated
        draft.copy(started = !shouldReset)
      }
    }

    private def addPartialPublishNote(
        draft: Draft,
        user: TokenUser,
        partialPublishFields: Set[PartialArticleFields]
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
        updatedApiArticle: api.UpdatedArticle
    ): Try[Draft] = {
      val fieldsToPartialPublish = shouldPartialPublish(oldArticle, toUpdate)
      val withPartialPublishNote = addPartialPublishNote(toUpdate, user, fieldsToPartialPublish)
      val withRevisionDateNotes  = addRevisionDateNotes(user, withPartialPublishNote, oldArticle)
      val withStarted = updateStartedField(withRevisionDateNotes, oldArticle, statusWasUpdated, updatedApiArticle)

      for {
        _ <- contentValidator.validateArticleOnLanguage(toUpdate, language)
        domainArticle <- performArticleUpdate(
          withStarted,
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
          language.getOrElse(Language.AllLanguages)
        )
        _ = indexArticle(domainArticle)
        _ <- updateTaxonomyForArticle(domainArticle)
      } yield domainArticle
    }

    private def updateTaxonomyForArticle(article: Draft) = {
      article.id match {
        case Some(id) => taxonomyApiClient.updateTaxonomyIfExists(id, article).map(_ => article)
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
          article.copy(
            revision = None,
            notes = Seq.empty,
            editorLabels = Seq.empty,
            created = LocalDateTime.MIN,
            updated = LocalDateTime.MIN,
            updatedBy = "",
            availability = common.Availability.everyone,
            grepCodes = Seq.empty,
            copyright = article.copyright.map(e => e.copy(license = None)),
            metaDescription = Seq.empty,
            relatedContent = Seq.empty,
            tags = Seq.empty,
            revisionMeta = Seq.empty,
            comments = List.empty,
            prioritized = false,
            started = false,
            // LanguageField ordering shouldn't matter:
            visualElement = article.visualElement.sorted,
            content = article.content.sorted,
            introduction = article.introduction.sorted,
            metaImage = article.metaImage.sorted,
            title = article.title.sorted
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
        field: api.PartialArticleFields,
        old: Draft,
        changed: Draft
    ): Option[api.PartialArticleFields] = {
      import api.PartialArticleFields._
      val shouldInclude = field match {
        case `availability`    => old.availability != changed.availability
        case `grepCodes`       => old.grepCodes != changed.grepCodes
        case `relatedContent`  => old.relatedContent != changed.relatedContent
        case `tags`            => old.tags.sorted != changed.tags.sorted
        case `metaDescription` => old.metaDescription.sorted != changed.metaDescription.sorted
        case `license`         => old.copyright.flatMap(_.license) != changed.copyright.flatMap(_.license)
        case `revisionDate`    => compareRevisionDates(old, changed)
      }

      Option.when(shouldInclude)(field)
    }

    /** Returns fields to publish _if_ partial-publishing requirements are satisfied, otherwise returns empty set. */
    private[service] def shouldPartialPublish(
        existingArticle: Option[Draft],
        changedArticle: Draft
    ): Set[api.PartialArticleFields] = {
      val isPublished =
        changedArticle.status.current == PUBLISHED ||
          changedArticle.status.other.contains(PUBLISHED)

      if (isPublished) {
        val changedFields = existingArticle
          .map(e => api.PartialArticleFields.values.flatMap(field => compareField(field, e, changedArticle)))
          .getOrElse(api.PartialArticleFields.values)

        changedFields.toSet
      } else {
        Set.empty
      }
    }

    private[service] def updateStatusIfNeeded(
        convertedArticle: Draft,
        existingArticle: Draft,
        updatedApiArticle: api.UpdatedArticle,
        user: TokenUser
    ): Try[Draft] = {
      val newManualStatus           = updatedApiArticle.status.traverse(DraftStatus.valueOfOrError).?
      val shouldNotAutoUpdateStatus = !shouldUpdateStatus(convertedArticle, existingArticle)
      if (shouldNotAutoUpdateStatus && newManualStatus.isEmpty)
        return Success(convertedArticle)

      val oldStatus            = existingArticle.status.current
      val newStatusIfUndefined = if (oldStatus == PUBLISHED) IN_PROGRESS else oldStatus
      val newStatus            = newManualStatus.getOrElse(newStatusIfUndefined)

      converterService
        .updateStatus(newStatus, convertedArticle, user, isImported = false)
        .attempt
        .unsafeRunSync()
        .toTry
        .flatten
    }

    def updateArticle(
        articleId: Long,
        updatedApiArticle: api.UpdatedArticle,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        user: TokenUser,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime],
        importId: Option[String]
    ): Try[api.Article] = {
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
        updatedApiArticle: api.UpdatedArticle,
        externalIds: List[String],
        externalSubjectIds: Seq[String],
        user: TokenUser,
        oldNdlaCreatedDate: Option[LocalDateTime],
        oldNdlaUpdatedDate: Option[LocalDateTime],
        importId: Option[String]
    ): Try[api.Article] = for {
      convertedArticle <- converterService.toDomainArticle(
        existing,
        updatedApiArticle,
        externalIds.nonEmpty,
        user,
        oldNdlaCreatedDate,
        oldNdlaUpdatedDate
      )
      articleWithStatus <- updateStatusIfNeeded(convertedArticle, existing, updatedApiArticle, user)
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
        updatedApiArticle = updatedApiArticle
      )
      apiArticle <- converterService.toApiArticle(
        readService.addUrlsOnEmbedResources(updatedArticle),
        updatedApiArticle.language.getOrElse(UnknownLanguage.toString),
        updatedApiArticle.language.isEmpty
      )
    } yield apiArticle

    def deleteLanguage(id: Long, language: String, userInfo: TokenUser): Try[api.Article] = {
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

    def deleteArticle(id: Long): Try[api.ContentId] = {
      draftRepository
        .deleteArticle(id)(AutoSession)
        .flatMap(articleIndexService.deleteDocument)
        .map(api.ContentId)
    }

    def storeFile(file: FileItem): Try[api.UploadedFile] =
      uploadFile(file).map(f => api.UploadedFile(f.fileName, f.contentType, f.fileExtension, s"/files/${f.filePath}"))

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

    def deleteFile(fileUrlOrPath: String): Try[_] = {
      val filePath = getFilePathFromUrl(fileUrlOrPath)
      if (fileStorage.resourceWithPathExists(filePath)) {
        fileStorage.deleteResourceWithPath(filePath)
      } else {
        Failure(api.NotFoundException(s"Could not find file with file path '$filePath' in storage."))
      }
    }

    private[service] def uploadFile(file: FileItem): Try[domain.UploadedFile] = {
      getFileExtension(file.name).flatMap(fileExtension => {
        val contentType = file.getContentType.getOrElse("")
        val fileName = LazyList
          .continually(randomFilename(fileExtension))
          .dropWhile(fileStorage.resourceExists)
          .head

        fileStorage
          .uploadResourceFromStream(new ByteArrayInputStream(file.get()), fileName, contentType, file.size)
          .map(uploadPath => domain.UploadedFile(fileName, uploadPath, file.size, contentType, fileExtension))
      })
    }

    private[service] def randomFilename(extension: String, length: Int = 20): String = {
      val extensionWithDot =
        if (!extension.headOption.contains('.') && extension.length > 0) s".$extension" else extension
      val randomString = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

    def newUserData(userId: String): Try[api.UserData] = {
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

    def updateUserData(updatedUserData: api.UpdatedUserData, user: TokenUser): Try[api.UserData] = {
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
        articleFieldsToUpdate: Seq[api.PartialArticleFields],
        language: String
    ): PartialPublishArticle = {
      val isAllLanguage  = language == Language.AllLanguages
      val initialPartial = PartialPublishArticle.empty()

      import api.PartialArticleFields._
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
        }
      })
    }

    def partialPublishAndConvertToApiArticle(
        id: Long,
        fieldsToPublish: Seq[api.PartialArticleFields],
        language: String,
        fallback: Boolean
    ): Try[api.Article] =
      partialPublish(id, fieldsToPublish, language)._2.flatMap(article =>
        converterService.toApiArticle(article, language, fallback)
      )

    def partialPublish(
        id: Long,
        articleFieldsToUpdate: Seq[api.PartialArticleFields],
        language: String
    ): (Long, Try[Draft]) =
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case None => id -> Failure(api.NotFoundException(s"Could not find draft with id of ${id} to partial publish"))
        case Some(article) =>
          partialPublish(article, articleFieldsToUpdate, language): Unit
          id -> Success(article)
      }

    private def partialPublishIfNeeded(
        article: Draft,
        articleFieldsToUpdate: Seq[api.PartialArticleFields],
        language: String
    ): Future[Try[Draft]] = {
      if (articleFieldsToUpdate.nonEmpty)
        partialPublish(article, articleFieldsToUpdate, language)
      else
        Future.successful(Success(article))
    }

    private def partialPublish(
        article: Draft,
        fieldsToPublish: Seq[api.PartialArticleFields],
        language: String
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
            articleApiClient.partialPublishArticle(id, partialArticle)
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
        partialBulk: api.PartialBulkArticles
    ): Try[api.MultiPartialPublishResult] = {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))
      val requestInfo = RequestInfo.fromThreadContext()

      val futures = partialBulk.articleIds.map(id =>
        Future {
          requestInfo.setThreadContextRequestInfo()
          partialPublish(id, partialBulk.fields, language)
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
          val failures = res.collect {
            case (id, Failure(ex)) => {
              logger.error(s"Partial publishing ${id} failed with ${ex.getMessage}", ex)
              api.PartialPublishFailure(id, ex.getMessage)
            }
          }

          Success(
            api.MultiPartialPublishResult(
              successes = successes,
              failures = failures
            )
          )
      }
    }

    private def getRevisionMetaForUrn(topic: Topic): Seq[common.draft.RevisionMeta] = {
      topic.contentUri match {
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
        case Failure(_) => Failure(api.NotFoundException(s"No topics with id ${publicId}"))
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

    def setRevisions(entity: Taxonomy[_], revisions: Seq[common.draft.RevisionMeta]): Try[_] = {
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
          case Topic(id, _, _, _) =>
            taxonomyApiClient
              .getChildResources(id)
              .flatMap(resources => resources.traverse(setRevisions(_, revisions)))
          case _ => Success(())
        }
      })
    }

    def updateArticleWithRevisions(articleId: Long, revisions: Seq[common.draft.RevisionMeta]): Try[_] = {
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

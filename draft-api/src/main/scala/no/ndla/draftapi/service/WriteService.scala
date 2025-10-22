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
import io.lemonlabs.uri.Path
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.Clock
import no.ndla.common.ContentURIUtil.parseArticleIdAndRevision
import no.ndla.common.TryUtil.failureIf
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.{MissingIdException, NotFoundException, OperationNotAllowedException, ValidationException}
import no.ndla.common.implicits.*
import no.ndla.common.logging.logTaskTime
import no.ndla.common.model.api.UpdateWith
import no.ndla.common.model.domain.article.PartialPublishArticleDTO
import no.ndla.common.model.domain.{EditorNote, Priority, Responsible, UploadedFile}
import no.ndla.common.model.domain.draft.DraftStatus.{IN_PROGRESS, PLANNED, PUBLISHED}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.database.DBUtility
import no.ndla.draftapi.DraftUtil.shouldPartialPublish
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration.*
import no.ndla.draftapi.model.api.{AddMultipleNotesDTO, AddNoteDTO, PartialArticleFieldsDTO}
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.search.{ArticleIndexService, TagIndexService}
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.language.Language
import no.ndla.language.Language.UnknownLanguage
import no.ndla.network.clients.SearchApiClient
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.*
import org.jsoup.nodes.Element
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import java.util.concurrent.Executors
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.jdk.CollectionConverters.*
import scala.math.max
import scala.util.{Failure, Random, Success, Try, boundary}

class WriteService(using
    draftRepository: DraftRepository,
    userDataRepository: UserDataRepository,
    converterService: => ConverterService,
    contentValidator: ContentValidator,
    articleIndexService: => ArticleIndexService,
    tagIndexService: => TagIndexService,
    clock: Clock,
    readService: => ReadService,
    articleApiClient: ArticleApiClient,
    searchApiClient: => SearchApiClient,
    fileStorage: => FileStorageService,
    taxonomyApiClient: TaxonomyApiClient,
    props: Props,
    dbUtility: DBUtility,
    stateTransitionRules: StateTransitionRules,
) extends StrictLogging {
  def insertDump(article: Draft): Try[Draft] = dbUtility.rollbackOnFailure(implicit session => {
    draftRepository
      .newEmptyArticleId()
      .map(newId => {
        val artWithId = article.copy(id = Some(newId))
        draftRepository.insert(artWithId)
      })
  })

  private def indexArticle(article: Draft, user: TokenUser): Try[Unit] = {
    val executor                                     = Executors.newSingleThreadExecutor
    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(executor)

    article.id match {
      case None            => Failure(new IllegalStateException("No id found for article when indexing. This is a bug."))
      case Some(articleId) =>
        val _ = searchApiClient.indexDocument("draft", article, Some(user))
        val _ = articleIndexService.indexAsync(articleId, article)
        val _ = tagIndexService.indexAsync(articleId, article)
        Success(())
    }

  }

  def copyArticleFromId(
      articleId: Long,
      userInfo: TokenUser,
      language: String,
      fallback: Boolean,
      usePostFix: Boolean,
  ): Try[api.ArticleDTO] = {
    dbUtility.rollbackOnFailure { implicit session =>
      draftRepository.withId(articleId) match {
        case None          => Failure(api.NotFoundException(s"Article with id '$articleId' was not found in database."))
        case Some(article) => for {
            newId <- draftRepository.newEmptyArticleId()
            status = common.Status(PLANNED, Set.empty)
            notes <- converterService.newNotes(
              Seq(s"Opprettet artikkel, som kopi av artikkel med id: '$articleId'."),
              userInfo,
              status,
            )
            newTitles =
              if (usePostFix) article.title.map(t => t.copy(title = t.title + " (Kopi)"))
              else article.title
            newContents    <- contentWithClonedFiles(article.content.toList)
            newResponsible  = Some(Responsible(userInfo.id, clock.now()))
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
              notes = notes,
            )
            inserted   = draftRepository.insert(articleToInsert)
            _          = indexArticle(inserted, userInfo)
            enriched   = readService.addUrlsOnEmbedResources(inserted)
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
      case Some(existingPath) => cloneFileAndGetNewPath(existingPath).map(newPath => {
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

  def newArticle(newArticle: api.NewArticleDTO, user: TokenUser): Try[api.ArticleDTO] = {
    val newNotes      = Some("Opprettet artikkel" +: newArticle.notes.getOrElse(Seq.empty))
    val visualElement = newArticle.visualElement.filter(_.nonEmpty)
    val withNotes     = newArticle.copy(notes = newNotes, visualElement = visualElement)
    dbUtility.rollbackOnFailure { implicit session =>
      for {
        newId           <- draftRepository.newEmptyArticleId()
        domainArticle   <- converterService.toDomainArticle(newId, withNotes, user)
        _               <- contentValidator.validateArticle(None, domainArticle)
        insertedArticle <- Try(draftRepository.insert(domainArticle))
        _                = indexArticle(insertedArticle, user)
        apiArticle      <- converterService.toApiArticle(insertedArticle, newArticle.language)
      } yield apiArticle
    }
  }

  def updateArticleStatus(status: DraftStatus, id: Long, user: TokenUser): Try[api.ArticleDTO] = {
    draftRepository.withId(id)(using ReadOnlyAutoSession) match {
      case None        => Failure(api.NotFoundException(s"No article with id $id was found"))
      case Some(draft) => for {
          convertedArticle <- stateTransitionRules.doTransition(draft, status, user)
          updatedArticle   <- updateArticleAndStoreAsNewIfPublished(convertedArticle, statusWasUpdated = true)
          _                 = indexArticle(updatedArticle, user)
          apiArticle       <- converterService.toApiArticle(updatedArticle, Language.AllLanguages, fallback = true)
        } yield apiArticle
    }
  }

  def updateArticleAndStoreAsNewIfPublished(article: Draft, statusWasUpdated: Boolean): Try[Draft] = {
    val storeAsNewVersion = statusWasUpdated && article.status.current == PUBLISHED
    dbUtility.rollbackOnFailure { implicit session =>
      draftRepository.updateArticle(article) match {
        case Success(updated) if storeAsNewVersion => draftRepository.storeArticleAsNewVersion(updated, None)
        case Success(updated)                      => Success(updated)
        case Failure(ex)                           => Failure(ex)
      }
    }
  }

  def getRanges(session: DBSession): Try[List[(Long, Long)]] = Try {
    val (minId, maxId) = draftRepository.minMaxArticleId(using session)
    Seq.range(minId, maxId + 1).grouped(100).map(group => (group.head, group.last)).toList
  }

  private val grepFieldsToPublish = Seq(PartialArticleFieldsDTO.grepCodes)

  private def getGrepCodeNote(mapping: Map[String, String], draft: Draft, user: TokenUser): EditorNote = {
    val grepCodes = mapping
      .map { case (old, newGrep) =>
        s"$old -> $newGrep"
      }
      .mkString(", ")
    common.EditorNote(s"Oppdaterte grep-koder: [$grepCodes]", user.id, draft.status, clock.now())
  }

  private def migrateOutdatedGrepForDraft(draft: Draft, user: TokenUser)(
      session: DBSession
  ): Try[Option[(Long, PartialPublishArticleDTO)]] = permitTry {
    boundary {
      val articleId = draft.id.getOrElse(-1L)
      logger.info(s"Migrating grep codes for article $articleId")
      if (draft.grepCodes.isEmpty) {
        boundary.break(Success(None))
      }
      val newGrepCodeMapping = searchApiClient.convertGrepCodes(draft.grepCodes, user).?
      val updatedGrepCodes   = newGrepCodeMapping.values.toSeq
      if (draft.grepCodes.sorted == updatedGrepCodes.sorted) {
        boundary.break(Success(None))
      }
      val grepCodeNote = getGrepCodeNote(newGrepCodeMapping, draft, user)
      val newDraft     = draft.copy(grepCodes = updatedGrepCodes, notes = draft.notes :+ grepCodeNote)
      val updated      = draftRepository.updateArticle(newDraft)(using session).?
      val partialPart  = partialArticleFieldsUpdate(updated, grepFieldsToPublish, Language.AllLanguages)
      logger.info(
        s"Migrated grep codes for article $articleId from [${draft.grepCodes.mkString(",")}] to [${updatedGrepCodes.mkString(",")}]"
      )
      lazy val idException = MissingIdException("Article id was missing after updating grep codes. This is a bug.")
      val id               = updated.id.toTry(idException).?
      Success(Some((id, partialPart)))
    }
  }

  def migrateOutdatedGreps(user: TokenUser): Try[Unit] = logTaskTime("Migrate outdated grep codes") {
    dbUtility.rollbackOnFailure { session =>
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))
      val result = getRanges(session).map(ranges => {
        val chunkResult = ranges.map { case (start, end) =>
          Future {
            val chunk = draftRepository.documentsWithArticleIdBetween(start, end)(using session)
            chunk.map(d => migrateOutdatedGrepForDraft(d, user)(session))
          }
        }
        chunkResult
      })

      result.flatMap { futures =>
        val fut     = Future.sequence(futures)
        val awaited = Try(Await.result(fut, 1.hour).flatten.sequence.map(_.flatten)).flatten

        awaited.flatMap { toPartialPublish =>
          articleApiClient.bulkPartialPublishArticles(toPartialPublish.toMap, user)
        }
      }
    }
  }

  /** Determines which repository function(s) should be called and calls them */
  private def performArticleUpdate(
      article: Draft,
      createNewVersion: Boolean,
      user: TokenUser,
      statusWasUpdated: Boolean,
  ): Try[Draft] =
    if (createNewVersion)
      draftRepository.storeArticleAsNewVersion(article, Some(user), keepDraftData = true)(using AutoSession)
    else updateArticleAndStoreAsNewIfPublished(article, statusWasUpdated)

  private def addRevisionDateNotes(user: TokenUser, updatedArticle: Draft, oldArticle: Option[Draft]): Draft = {
    val oldRevisions = oldArticle.map(a => a.revisionMeta).getOrElse(Seq.empty)
    val oldIds       = oldRevisions.map(rm => rm.id).toSet
    val newIds       = updatedArticle.revisionMeta.map(rm => rm.id).toSet
    val deleted      = oldRevisions
      .filterNot(old => newIds.contains(old.id))
      .map(del => common.EditorNote(s"Slettet revisjon ${del.note}.", user.id, updatedArticle.status, clock.now()))

    val notes = updatedArticle
      .revisionMeta
      .flatMap {
        case rm if !oldIds.contains(rm.id) && rm.status == common.RevisionStatus.Revised =>
          common
            .EditorNote(s"Lagt til og fullført revisjon ${rm.note}.", user.id, updatedArticle.status, clock.now())
            .some
        case rm if !oldIds.contains(rm.id) =>
          common.EditorNote(s"Lagt til revisjon ${rm.note}.", user.id, updatedArticle.status, clock.now()).some
        case rm => oldRevisions.find(_.id == rm.id) match {
            case Some(old) if old.status != rm.status && rm.status == common.RevisionStatus.Revised =>
              common.EditorNote(s"Fullført revisjon ${rm.note}.", user.id, updatedArticle.status, clock.now()).some
            case Some(old) if old != rm =>
              common.EditorNote(s"Endret revisjon ${rm.note}.", user.id, updatedArticle.status, clock.now()).some
            case _ => None
          }
      }

    updatedArticle.copy(notes = updatedArticle.notes ++ notes ++ deleted)
  }

  private def hasResponsibleBeenUpdated(draft: Draft, oldDraft: Option[Draft]): Boolean = {
    draft.responsible match {
      case None              => false
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
      shouldNotAutoUpdateStatus: Boolean,
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

  private def updatePriorityField(draft: Draft, oldDraft: Option[Draft], statusWasUpdated: Boolean): Draft = {
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
      partialPublishFields: Set[PartialArticleFieldsDTO],
  ): Draft =
    if (partialPublishFields.nonEmpty) converterService.addNote(draft, "Artikkelen har blitt delpublisert", user)
    else draft

  private def updateArticle(
      toUpdate: Draft,
      language: Option[String],
      createNewVersion: Boolean,
      oldArticle: Option[Draft],
      user: TokenUser,
      statusWasUpdated: Boolean,
      updatedApiArticle: api.UpdatedArticleDTO,
      shouldNotAutoUpdateStatus: Boolean,
  ): Try[Draft] = {
    val fieldsToPartialPublish = shouldPartialPublish(oldArticle, toUpdate)
    val withPartialPublishNote = addPartialPublishNote(toUpdate, user, fieldsToPartialPublish)
    val withRevisionDateNotes  = addRevisionDateNotes(user, withPartialPublishNote, oldArticle)
    val withStarted            = updateStartedField(
      withRevisionDateNotes,
      oldArticle,
      statusWasUpdated,
      updatedApiArticle,
      shouldNotAutoUpdateStatus,
    )
    val withPriority  = updatePriorityField(withStarted, oldArticle, statusWasUpdated)
    val languageOrAll = language.getOrElse(Language.AllLanguages)

    for {
      _             <- contentValidator.validateArticleOnLanguage(oldArticle, toUpdate, language)
      domainArticle <- performArticleUpdate(withPriority, createNewVersion, user, statusWasUpdated)
      _              = partialPublishIfNeeded(domainArticle, fieldsToPartialPublish.toSeq, languageOrAll, user)
      _              = indexArticle(domainArticle, user)
      _             <- updateTaxonomyForArticle(domainArticle, user)
    } yield domainArticle
  }

  private def updateTaxonomyForArticle(article: Draft, user: TokenUser) = {
    article.id match {
      case Some(id) => taxonomyApiClient.updateTaxonomyIfExists(id, article, user).map(_ => article)
      case None     => Failure(
          api.ArticleVersioningException("Article supplied to taxonomy update did not have an id. This is a bug.")
        )
    }
  }

  def shouldUpdateStatus(changedArticle: Draft, existingArticle: Draft): Boolean = {
    // Function that sets values we don't want to include when comparing articles to check if we should update status
    val withComparableValues = (article: Draft) =>
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
          qualityEvaluation = None,
        )

    val comparableNew      = withComparableValues(changedArticle)
    val comparableExisting = withComparableValues(existingArticle)
    val shouldUpdateStatus = comparableNew != comparableExisting
    shouldUpdateStatus
  }

  private def flattenNotes(notes: AddMultipleNotesDTO): List[AddNoteDTO] = notes
    .data
    .groupBy(_.draftId)
    .map { case (draftId, notes) =>
      AddNoteDTO(draftId, notes.flatMap(_.notes))
    }
    .toList

  def addNotesToDrafts(input: AddMultipleNotesDTO, user: TokenUser): Try[Unit] = dbUtility.rollbackOnFailure {
    session =>
      flattenNotes(input).traverse(info => addNotesToDraft(info.draftId, info.notes, user)(session)).unit
  }

  private def addNotesToDraft(id: Long, notes: List[String], user: TokenUser)(session: DBSession): Try[Boolean] = {
    for {
      maybeDraft <- Try(draftRepository.withId(id)(using session))
      draft      <- maybeDraft.toTry(NotFoundException(s"Article with id $id not found"))
      now         = clock.now()
      newNotes    = notes.map(note => common.EditorNote(note, user.id, draft.status, now))
      result      = draftRepository.updateArticleNotes(id, newNotes)(using session)
    } yield result.isSuccess
  }

  private[service] def updateStatusIfNeeded(
      convertedArticle: Draft,
      existingArticle: Draft,
      updatedApiArticle: api.UpdatedArticleDTO,
      user: TokenUser,
      shouldNotAutoUpdateStatus: Boolean,
  ): Try[Draft] = permitTry {
    val newManualStatus = updatedApiArticle.status.traverse(DraftStatus.valueOfOrError).?
    if (shouldNotAutoUpdateStatus && newManualStatus.isEmpty) {
      Success(convertedArticle)
    } else {
      val oldStatus            = existingArticle.status.current
      val newStatusIfUndefined =
        if (oldStatus == PUBLISHED) IN_PROGRESS
        else oldStatus
      val newStatus = newManualStatus.getOrElse(newStatusIfUndefined)

      stateTransitionRules.doTransition(convertedArticle, newStatus, user)
    }
  }

  def updateArticle(articleId: Long, updatedApiArticle: api.UpdatedArticleDTO, user: TokenUser): Try[api.ArticleDTO] =
    draftRepository.withId(articleId)(using ReadOnlyAutoSession) match {
      case Some(existing) => updateExistingArticle(existing, updatedApiArticle, user)
      case None           => Failure(api.NotFoundException(s"Article with id $articleId does not exist"))
    }

  private def updateExistingArticle(
      existing: Draft,
      updatedApiArticle: api.UpdatedArticleDTO,
      user: TokenUser,
  ): Try[api.ArticleDTO] = for {
    convertedArticle         <- converterService.toDomainArticle(existing, updatedApiArticle, user)
    shouldNotAutoUpdateStatus = !shouldUpdateStatus(convertedArticle, existing)

    articleWithStatus <-
      updateStatusIfNeeded(convertedArticle, existing, updatedApiArticle, user, shouldNotAutoUpdateStatus)

    didUpdateStatus = articleWithStatus.status.current != convertedArticle.status.current

    updatedArticle <- updateArticle(
      articleWithStatus,
      language = updatedApiArticle.language,
      createNewVersion = updatedApiArticle.createNewVersion.getOrElse(false),
      oldArticle = Some(existing),
      user = user,
      statusWasUpdated = didUpdateStatus,
      updatedApiArticle = updatedApiArticle,
      shouldNotAutoUpdateStatus = shouldNotAutoUpdateStatus,
    )
    withEmbedUrls = readService.addUrlsOnEmbedResources(updatedArticle)

    apiArticle <- converterService.toApiArticle(
      article = withEmbedUrls,
      language = updatedApiArticle.language.getOrElse(UnknownLanguage.toString),
      fallback = updatedApiArticle.language.isEmpty,
    )
  } yield apiArticle

  def deleteLanguage(id: Long, language: String, userInfo: TokenUser): Try[api.ArticleDTO] = {
    draftRepository.withId(id)(using ReadOnlyAutoSession) match {
      case Some(article) => article.title.size match {
          case 1 => Failure(OperationNotAllowedException("Only one language left"))
          case _ => for {
              newArticle <- converterService.deleteLanguage(article, language, userInfo)
              stored     <- updateArticleAndStoreAsNewIfPublished(newArticle, statusWasUpdated = true)
              converted  <- converterService.toApiArticle(stored, Language.AllLanguages)
            } yield converted
        }
      case None => Failure(api.NotFoundException("Article does not exist"))
    }
  }

  def deleteArticle(id: Long): Try[api.ContentIdDTO] = {
    draftRepository
      .deleteArticle(id)(using AutoSession)
      .flatMap(articleIndexService.deleteDocument)
      .map(id => api.ContentIdDTO(id))
  }

  def storeFile(file: UploadedFile): Try[api.UploadedFileDTO] = uploadFile(file).map(f =>
    api.UploadedFileDTO(
      filename = f.fileName,
      mime = f.contentType,
      extension = f.fileExtension,
      path = s"/files/${f.filePath}",
    )
  )

  private[service] def getFileExtension(fileName: String): Try[String] = {
    val badExtensionError = ValidationException(
      "file",
      s"The file must have one of the supported file extensions: '${props.supportedUploadExtensions.mkString(", ")}'",
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
    filePath.path.parts.dropWhile(_ == "files").mkString("/")
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
          fileExtension = fileExtension,
        )
      )
  }

  private[service] def randomFilename(extension: String, length: Int = 20): String = {
    val extensionWithDot =
      if (!extension.headOption.contains('.') && extension.nonEmpty) s".$extension"
      else extension
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
          latestEditedLearningpaths = None,
          favoriteSubjects = None,
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
          latestEditedLearningpaths = updatedUserData.latestEditedLearningpaths,
          favoriteSubjects = updatedUserData.favoriteSubjects,
        )
        userDataRepository.insert(newUserData).map(converterService.toApiUserData)

      case Some(existing) =>
        val toUpdate = existing.copy(
          savedSearches = updatedUserData.savedSearches.orElse(existing.savedSearches),
          latestEditedArticles = updatedUserData.latestEditedArticles.orElse(existing.latestEditedArticles),
          latestEditedConcepts = updatedUserData.latestEditedConcepts.orElse(existing.latestEditedConcepts),
          latestEditedLearningpaths =
            updatedUserData.latestEditedLearningpaths.orElse(existing.latestEditedLearningpaths),
          favoriteSubjects = updatedUserData.favoriteSubjects.orElse(existing.favoriteSubjects),
        )
        userDataRepository.update(toUpdate).map(converterService.toApiUserData)
    }
  }

  private[service] def partialArticleFieldsUpdate(
      article: Draft,
      articleFieldsToUpdate: Seq[api.PartialArticleFieldsDTO],
      language: String,
  ): PartialPublishArticleDTO = {
    val isAllLanguage  = language == Language.AllLanguages
    val initialPartial = PartialPublishArticle.empty()

    import api.PartialArticleFieldsDTO.*
    articleFieldsToUpdate
      .distinct
      .foldLeft(initialPartial)((partial, field) => {
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
      user: TokenUser,
  ): Try[api.ArticleDTO] = partialPublish(id, fieldsToPublish, language, user)
    ._2
    .flatMap(article => converterService.toApiArticle(article, language, fallback))

  def partialPublish(
      id: Long,
      articleFieldsToUpdate: Seq[api.PartialArticleFieldsDTO],
      language: String,
      user: TokenUser,
  ): (Long, Try[Draft]) = draftRepository.withId(id)(using ReadOnlyAutoSession) match {
    case None          => id -> Failure(api.NotFoundException(s"Could not find draft with id of $id to partial publish"))
    case Some(article) =>
      partialPublish(article, articleFieldsToUpdate, language, user): Unit
      id -> Success(article)
  }

  private def partialPublishIfNeeded(
      article: Draft,
      articleFieldsToUpdate: Seq[api.PartialArticleFieldsDTO],
      language: String,
      user: TokenUser,
  ): Future[Try[Draft]] = {
    if (articleFieldsToUpdate.nonEmpty) partialPublish(article, articleFieldsToUpdate, language, user)
    else Future.successful(Success(article))
  }

  private def partialPublish(
      article: Draft,
      fieldsToPublish: Seq[api.PartialArticleFieldsDTO],
      language: String,
      user: TokenUser,
  ): Future[Try[Draft]] = {
    article.id match {
      case None => Future.successful(
          Failure(new IllegalStateException(s"Article to partial publish did not have id. This is a bug."))
        )
      case Some(id) =>
        implicit val executionContext: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))
        val partialArticle = partialArticleFieldsUpdate(article, fieldsToPublish, language)
        val requestInfo    = RequestInfo.fromThreadContext()
        val fut            = Future {
          requestInfo.setThreadContextRequestInfo()
          articleApiClient.partialPublishArticle(id, partialArticle, user)
        }

        val logError =
          (ex: Throwable) => logger.error(s"Failed to partial publish article with id '$id', with error", ex)

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
      user: TokenUser,
  ): Try[api.MultiPartialPublishResultDTO] = {
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))
    val requestInfo = RequestInfo.fromThreadContext()

    val futures = partialBulk
      .articleIds
      .map(id =>
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
        val successes = res.collect { case (id, Success(_)) =>
          id
        }
        val failures = res.collect { case (id, Failure(ex)) =>
          logger.error(s"Partial publishing $id failed with ${ex.getMessage}", ex)
          api.PartialPublishFailureDTO(id, ex.getMessage)
        }

        Success(api.MultiPartialPublishResultDTO(successes = successes, failures = failures))
    }
  }

  private def getRevisionMetaForUrn(node: Node): Seq[common.RevisionMeta] = {
    node.contentUri match {
      case Some(contentUri) => parseArticleIdAndRevision(contentUri) match {
          case (Success(articleId), _) => draftRepository.withId(articleId)(using ReadOnlyAutoSession) match {
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
      case Failure(_)     => Failure(api.NotFoundException(s"No topics with id $publicId"))
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

  def deleteCurrentRevision(id: Long): Try[Unit] = dbUtility.rollbackOnFailure { implicit session =>
    lazy val missingRevisionError = api.NotFoundException(s"No revision found for article with id $id")
    lazy val partialPublishError  = OperationNotAllowedException("The previous revision has been partially published")
    lazy val publishedDeleteError = OperationNotAllowedException("Cannot delete a published revision")
    for {
      (current, previous) <- draftRepository.getCurrentAndPreviousRevision(id)
      revision            <- current.revision.toTry(missingRevisionError)
      _                   <- failureIf(shouldPartialPublish(Some(previous), current).nonEmpty, partialPublishError)
      _                   <- failureIf(current.status.current == PUBLISHED, publishedDeleteError)
      _                   <- draftRepository.deleteArticleRevision(id, revision)
      _                   <-
        if (previous.status.current == PUBLISHED) {
          draftRepository.storeArticleAsNewVersion(previous, None)
        } else {
          Success(())
        }
    } yield ()
  }

  private def setRevisions(entity: Node, revisions: Seq[common.RevisionMeta]): Try[?] = {
    val updateResult = entity.contentUri match {
      case Some(contentUri) => parseArticleIdAndRevision(contentUri) match {
          case (Success(articleId), _) => updateArticleWithRevisions(articleId, revisions)
          case _                       => Success(())
        }
      case _ => Success(())
    }
    updateResult.map(_ => {
      entity match {
        case Node(id, _, _, _) =>
          taxonomyApiClient.getChildResources(id).flatMap(resources => resources.traverse(setRevisions(_, revisions)))
        case null => Success(())
      }
    })
  }

  private def updateArticleWithRevisions(articleId: Long, revisions: Seq[common.RevisionMeta]): Try[?] = {
    draftRepository
      .withId(articleId)(using ReadOnlyAutoSession)
      .traverse(article => {
        val revisionMeta = article.revisionMeta ++ revisions
        val toUpdate     = article.copy(revisionMeta = revisionMeta.distinct)
        draftRepository.updateArticle(toUpdate)(using AutoSession)
      })
  }
}

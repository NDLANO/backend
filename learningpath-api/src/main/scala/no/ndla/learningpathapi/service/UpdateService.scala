/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import cats.implicits._
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.implicits._
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.{SearchApiClient, TaxonomyApiClient}
import no.ndla.learningpathapi.model.api.{config, _}
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.model.domain.FolderSortObject.{FolderSorting, ResourceSorting, RootFolderSorting}
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathTokenUser
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import no.ndla.learningpathapi.model.domain.{Folder, LearningPathStatus, LearningPath => _, LearningStep => _, _}
import no.ndla.learningpathapi.repository.{
  ConfigRepository,
  FolderRepository,
  LearningPathRepositoryComponent,
  UserRepository
}
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait UpdateService {
  this: LearningPathRepositoryComponent
    with ReadService
    with ConfigRepository
    with FolderRepository
    with UserRepository
    with ConverterService
    with SearchIndexService
    with Clock
    with LearningStepValidator
    with LearningPathValidator
    with TaxonomyApiClient
    with FeideApiClient
    with SearchApiClient
    with Props
    with RedisClient =>
  val updateService: UpdateService

  class UpdateService {

    def updateTaxonomyForLearningPath(
        pathId: Long,
        createResourceIfMissing: Boolean,
        language: String,
        fallback: Boolean,
        userInfo: TokenUser
    ): Try[LearningPathV2] = {
      writeOrAccessDenied(userInfo.isWriter) {
        readService.withIdAndAccessGranted(pathId, userInfo) match {
          case Failure(ex) => Failure(ex)
          case Success(lp) =>
            taxonomyApiClient
              .updateTaxonomyForLearningPath(lp, createResourceIfMissing, Some(userInfo))
              .flatMap(l => converterService.asApiLearningpathV2(l, language, fallback, userInfo))
        }
      }
    }

    def insertDump(dump: domain.LearningPath): domain.LearningPath = learningPathRepository.insert(dump)

    private[service] def writeDuringWriteRestrictionOrAccessDenied[T](owner: TokenUser)(w: => Try[T]): Try[T] =
      writeOrAccessDenied(
        readService.canWriteNow(owner),
        "You do not have write access while write restriction is active."
      )(w)

    private def canWriteNow(myNDLAUser: domain.MyNDLAUser): Boolean =
      myNDLAUser.isTeacher || !readService.isMyNDLAWriteRestricted

    private[service] def writeOrAccessDenied[T](
        willExecute: Boolean,
        reason: String = "You do not have permission to perform this action."
    )(w: => Try[T]): Try[T] =
      if (willExecute) w
      else Failure(AccessDeniedException(reason))

    def newFromExistingV2(id: Long, newLearningPath: NewCopyLearningPathV2, owner: TokenUser): Try[LearningPathV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        learningPathRepository.withId(id).map(_.isOwnerOrPublic(owner)) match {
          case None              => Failure(NotFoundException("Could not find learningpath to copy."))
          case Some(Failure(ex)) => Failure(ex)
          case Some(Success(existing)) =>
            val toInsert = converterService.newFromExistingLearningPath(existing, newLearningPath, owner)
            learningPathValidator.validate(toInsert, allowUnknownLanguage = true)
            converterService.asApiLearningpathV2(
              learningPathRepository.insert(toInsert),
              newLearningPath.language,
              fallback = true,
              owner
            )
        }
      }

    def addLearningPathV2(newLearningPath: NewLearningPathV2, owner: TokenUser): Try[LearningPathV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        val learningPath = converterService.newLearningPath(newLearningPath, owner)
        learningPathValidator.validate(learningPath)

        converterService.asApiLearningpathV2(
          learningPathRepository.insert(learningPath),
          newLearningPath.language,
          fallback = true,
          owner
        )
      }

    def updateLearningPathV2(
        id: Long,
        learningPathToUpdate: UpdatedLearningPathV2,
        owner: TokenUser
    ): Try[LearningPathV2] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      learningPathValidator.validate(learningPathToUpdate)

      withId(id).flatMap(_.canEditLearningpath(owner)) match {
        case Failure(ex) => Failure(ex)
        case Success(existing) =>
          val toUpdate = converterService.mergeLearningPaths(existing, learningPathToUpdate, owner)

          // Imported learningpaths may contain fields with language=unknown.
          // We should still be able to update it, but not add new fields with language=unknown.
          learningPathValidator.validate(toUpdate, allowUnknownLanguage = true)

          val updatedLearningPath = learningPathRepository.update(toUpdate)

          updateSearchAndTaxonomy(updatedLearningPath, owner.some).flatMap(_ =>
            converterService.asApiLearningpathV2(
              updatedLearningPath,
              learningPathToUpdate.language,
              fallback = true,
              owner
            )
          )
      }
    }

    private def updateSearchAndTaxonomy(learningPath: domain.LearningPath, user: Option[TokenUser]) = {
      val sRes = searchIndexService.indexDocument(learningPath)

      if (learningPath.isPublished) {
        searchApiClient.indexLearningPathDocument(learningPath, user): Unit
      } else {
        deleteIsBasedOnReference(learningPath): Unit
      }

      sRes.flatMap(lp => taxonomyApiClient.updateTaxonomyForLearningPath(lp, createResourceIfMissing = false, user))
    }

    def updateLearningPathStatusV2(
        learningPathId: Long,
        status: LearningPathStatus.Value,
        owner: TokenUser,
        language: String,
        message: Option[String] = None
    ): Try[LearningPathV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        withId(learningPathId, includeDeleted = true).flatMap(_.canSetStatus(status, owner)) match {
          case Failure(ex) => Failure(ex)
          case Success(existing) =>
            val validatedLearningPath =
              if (status == domain.LearningPathStatus.PUBLISHED) existing.validateForPublishing() else Success(existing)

            validatedLearningPath.flatMap(valid => {
              val newMessage = message match {
                case Some(msg) if owner.isAdmin => Some(domain.Message(msg, owner.id, clock.now()))
                case _                          => valid.message
              }

              val updatedLearningPath = learningPathRepository.update(
                valid.copy(message = newMessage, status = status, lastUpdated = clock.now())
              )

              updateSearchAndTaxonomy(updatedLearningPath, owner.some)
                .flatMap(_ =>
                  converterService.asApiLearningpathV2(
                    updatedLearningPath,
                    language,
                    fallback = true,
                    owner
                  )
                )

            })
        }
      }

    private[service] def deleteIsBasedOnReference(updatedLearningPath: domain.LearningPath): Unit = {
      learningPathRepository
        .learningPathsWithIsBasedOn(updatedLearningPath.id.get)
        .foreach(lp => {
          learningPathRepository.update(
            lp.copy(
              lastUpdated = clock.now(),
              isBasedOn = None
            )
          )
        })
    }

    def addLearningStepV2(
        learningPathId: Long,
        newLearningStep: NewLearningStepV2,
        owner: TokenUser
    ): Try[LearningStepV2] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      optimisticLockRetries(10) {
        withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
          case Failure(ex) => Failure(ex)
          case Success(learningPath) =>
            val validated = for {
              newStep   <- converterService.asDomainLearningStep(newLearningStep, learningPath)
              validated <- learningStepValidator.validate(newStep)
            } yield validated

            validated match {
              case Failure(ex) => Failure(ex)
              case Success(newStep) =>
                val (insertedStep, updatedPath) = inTransaction { implicit session =>
                  val insertedStep =
                    learningPathRepository.insertLearningStep(newStep)
                  val toUpdate    = converterService.insertLearningStep(learningPath, insertedStep, owner)
                  val updatedPath = learningPathRepository.update(toUpdate)

                  (insertedStep, updatedPath)
                }

                updateSearchAndTaxonomy(updatedPath, owner.some)
                  .flatMap(_ =>
                    converterService.asApiLearningStepV2(
                      insertedStep,
                      updatedPath,
                      newLearningStep.language,
                      fallback = true,
                      owner
                    )
                  )
            }
        }
      }
    }

    def updateLearningStepV2(
        learningPathId: Long,
        learningStepId: Long,
        learningStepToUpdate: UpdatedLearningStepV2,
        owner: TokenUser
    ): Try[LearningStepV2] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
        case Failure(ex) => Failure(ex)
        case Success(learningPath) =>
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None =>
              Failure(
                NotFoundException(
                  s"Could not find learningstep with id '$learningStepId' to update with learningpath id '$learningPathId'."
                )
              )
            case Some(existing) =>
              val validated = for {
                toUpdate  <- converterService.mergeLearningSteps(existing, learningStepToUpdate)
                validated <- learningStepValidator.validate(toUpdate, allowUnknownLanguage = true)
              } yield validated

              validated match {
                case Failure(ex) => Failure(ex)
                case Success(toUpdate) =>
                  learningStepValidator.validate(toUpdate, allowUnknownLanguage = true).??

                  val (updatedStep, updatedPath) = inTransaction { implicit session =>
                    val updatedStep =
                      learningPathRepository.updateLearningStep(toUpdate)
                    val pathToUpdate = converterService.insertLearningStep(learningPath, updatedStep, owner)
                    val updatedPath  = learningPathRepository.update(pathToUpdate)

                    (updatedStep, updatedPath)
                  }

                  updateSearchAndTaxonomy(updatedPath, owner.some)
                    .flatMap(_ =>
                      converterService.asApiLearningStepV2(
                        updatedStep,
                        updatedPath,
                        learningStepToUpdate.language,
                        fallback = true,
                        owner
                      )
                    )
              }

          }
      }
    }

    def updateLearningStepStatusV2(
        learningPathId: Long,
        learningStepId: Long,
        newStatus: StepStatus,
        owner: TokenUser
    ): Try[LearningStepV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
          case Failure(ex) => Failure(ex)
          case Success(learningPath) =>
            learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
              case None =>
                Failure(
                  NotFoundException(
                    s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
                  )
                )
              case Some(learningStep) =>
                val stepToUpdate = learningStep.copy(status = newStatus)
                val stepsToChangeSeqNoOn = learningPathRepository
                  .learningStepsFor(learningPathId)
                  .filter(step => step.seqNo >= stepToUpdate.seqNo && step.id != stepToUpdate.id)

                val stepsWithChangedSeqNo = stepToUpdate.status match {
                  case StepStatus.DELETED =>
                    stepsToChangeSeqNoOn.map(step => step.copy(seqNo = step.seqNo - 1))
                  case StepStatus.ACTIVE =>
                    stepsToChangeSeqNoOn.map(step => step.copy(seqNo = step.seqNo + 1))
                }

                val (updatedPath, updatedStep) = inTransaction { implicit session =>
                  val updatedStep = learningPathRepository.updateLearningStep(stepToUpdate)

                  val newLearningSteps = learningPath.learningsteps
                    .getOrElse(Seq.empty)
                    .filterNot(step =>
                      stepsWithChangedSeqNo
                        .map(_.id)
                        .contains(step.id)
                    ) ++ stepsWithChangedSeqNo

                  val lp          = converterService.insertLearningSteps(learningPath, newLearningSteps, owner)
                  val updatedPath = learningPathRepository.update(lp)

                  stepsWithChangedSeqNo.foreach(learningPathRepository.updateLearningStep)

                  (updatedPath, updatedStep)
                }

                updateSearchAndTaxonomy(updatedPath, owner.some).flatMap(_ =>
                  converterService.asApiLearningStepV2(
                    updatedStep,
                    updatedPath,
                    props.DefaultLanguage,
                    fallback = true,
                    owner
                  )
                )

            }
        }
      }

    def updateConfig(
        configKey: ConfigKey,
        value: api.config.ConfigMetaValue,
        userInfo: TokenUser
    ): Try[config.ConfigMeta] = {
      writeOrAccessDenied(userInfo.isAdmin, "Only administrators can edit configuration.") {
        val config = ConfigMeta(configKey, domain.config.ConfigMetaValue.from(value), clock.now(), userInfo.id)
        for {
          validated <- config.validate
          stored    <- configRepository.updateConfigParam(validated)
        } yield converterService.asApiConfig(stored)
      }
    }

    def updateSeqNo(learningPathId: Long, learningStepId: Long, seqNo: Int, owner: TokenUser): Try[LearningStepSeqNo] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        optimisticLockRetries(10) {
          withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
            case Failure(ex) => Failure(ex)
            case Success(learningPath) =>
              learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
                case None =>
                  Failure(
                    NotFoundException(s"LearningStep with id $learningStepId in learningPath $learningPathId not found")
                  )
                case Some(learningStep) =>
                  learningPath.validateSeqNo(seqNo)

                  val from = learningStep.seqNo
                  val to   = seqNo
                  val toUpdate = learningPath.learningsteps
                    .getOrElse(Seq.empty)
                    .filter(step => rangeToUpdate(from, to).contains(step.seqNo))

                  def addOrSubtract(seqNo: Int): Int = if (from > to) seqNo + 1 else seqNo - 1

                  inTransaction { implicit session =>
                    learningPathRepository.updateLearningStep(learningStep.copy(seqNo = seqNo)): Unit
                    toUpdate.foreach(step => {
                      learningPathRepository.updateLearningStep(step.copy(seqNo = addOrSubtract(step.seqNo)))
                    })
                  }

                  Success(LearningStepSeqNo(seqNo))
              }
          }
        }
      }

    private def rangeToUpdate(from: Int, to: Int): Range = if (from > to) to until from else from + 1 to to

    private def withId(learningPathId: Long, includeDeleted: Boolean = false): Try[domain.LearningPath] = {
      val lpOpt = if (includeDeleted) {
        learningPathRepository.withIdIncludingDeleted(learningPathId)
      } else {
        learningPathRepository.withId(learningPathId)
      }

      lpOpt match {
        case Some(learningPath) => Success(learningPath)
        case None               => Failure(NotFoundException(s"Could not find learningpath with id '$learningPathId'."))
      }
    }

    @tailrec
    private def optimisticLockRetries[T](n: Int)(fn: => T): T = {
      try {
        fn
      } catch {
        case ole: OptimisticLockException =>
          if (n > 1) optimisticLockRetries(n - 1)(fn) else throw ole
        case t: Throwable => throw t
      }
    }

    private def getMyNDLAUser(feideId: FeideID, feideAccessToken: Option[FeideAccessToken]): Try[domain.MyNDLAUser] = {
      readService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(AutoSession)
    }

    private[service] def canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[_] = {
      getMyNDLAUser(feideId, feideAccessToken)
        .flatMap(myNDLAUser => {
          if (canWriteNow(myNDLAUser)) Success(())
          else Failure(AccessDeniedException("You do not have write access while write restriction is active."))
        })
    }

    private[service] def isOperationAllowedOrAccessDenied(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        updatedFolder: UpdatedFolder
    ): Try[_] = {
      getMyNDLAUser(feideId, feideAccessToken).flatMap(myNDLAUser => {
        if (myNDLAUser.isStudent && updatedFolder.status.contains(FolderStatus.SHARED.toString))
          Failure(AccessDeniedException("You do not have necessary permissions to share folders."))
        else if (!canWriteNow(myNDLAUser))
          Failure(AccessDeniedException("You do not have write access while write restriction is active."))
        else Success(())
      })
    }

    private def isTeacherOrAccessDenied(feideId: FeideID, feideAccessToken: Option[FeideAccessToken]): Try[_] = {
      getMyNDLAUser(feideId, feideAccessToken)
        .flatMap(myNDLAUser => {
          if (myNDLAUser.isTeacher) Success(())
          else Failure(AccessDeniedException("You do not have necessary permissions to share folders."))
        })
    }

    private def validateParentId(parentId: Option[UUID], parent: Option[domain.Folder]): Try[Option[UUID]] =
      (parentId, parent) match {
        case (Some(_), None) =>
          val paramName = "parentId"
          Failure(
            ValidationException(
              paramName,
              s"Invalid value for $paramName. The UUID specified does not exist or is not writable by you."
            )
          )
        case _ => Success(parentId)
      }

    private def checkDepth(parentId: Option[UUID]): Try[Unit] = {
      parentId match {
        case None => Success(())
        case Some(pid) =>
          folderRepository.getFoldersDepth(pid) match {
            case Failure(ex) => Failure(ex)
            case Success(currentDepth) if currentDepth >= props.MaxFolderDepth =>
              Failure(
                ValidationException(
                  "MAX_DEPTH_LIMIT_REACHED",
                  s"Folder can not be created, max folder depth limit of ${props.MaxFolderDepth} reached."
                )
              )
            case _ => Success(())
          }
      }
    }

    private def getFolderWithDirectChildren(maybeParentId: Option[UUID], feideId: FeideID)(implicit
        session: DBSession
    ): Try[FolderAndDirectChildren] = maybeParentId match {
      case None =>
        folderRepository
          .foldersWithFeideAndParentID(None, feideId)
          .map(siblingFolders => {
            FolderAndDirectChildren(None, siblingFolders, Seq.empty)
          })
      case Some(parentId) =>
        folderRepository.folderWithFeideId(parentId, feideId) match {
          case Failure(ex) => Failure(ex)
          case Success(parent) =>
            for {
              siblingFolders   <- folderRepository.foldersWithFeideAndParentID(parentId.some, feideId)
              siblingResources <- folderRepository.getConnections(parentId)
            } yield FolderAndDirectChildren(Some(parent), siblingFolders, siblingResources)
        }
    }

    private def validateSiblingNames(
        name: String,
        maybeParentAndSiblings: FolderAndDirectChildren
    ): Try[Unit] = {
      val FolderAndDirectChildren(_, siblings, _) = maybeParentAndSiblings
      val hasNameDuplicate                        = siblings.map(_.name).exists(_.toLowerCase == name.toLowerCase)
      if (hasNameDuplicate) {
        Failure(ValidationException("name", s"The folder name must be unique within its parent."))
      } else Success(())
    }

    private def getMaybeParentId(parentId: Option[String]): Try[Option[UUID]] = {
      parentId.traverse(pid => converterService.toUUIDValidated(pid.some, "parentId"))
    }

    private def validateUpdatedFolder(
        folderName: String,
        parentId: Option[UUID],
        maybeParentAndSiblings: FolderAndDirectChildren,
        updatedFolder: Folder
    ): Try[Option[UUID]] = {
      val folderTreeWithoutTheUpdatee = maybeParentAndSiblings.withoutChild(updatedFolder.id)
      for {
        validatedParentId <- validateParentId(parentId, maybeParentAndSiblings.folder)
        _                 <- validateSiblingNames(folderName, folderTreeWithoutTheUpdatee)
        _                 <- checkDepth(validatedParentId)
      } yield validatedParentId
    }

    private def validateNewFolder(
        folderName: String,
        parentId: Option[UUID],
        maybeParentAndSiblings: FolderAndDirectChildren
    ): Try[Option[UUID]] = for {
      validatedParentId <- validateParentId(parentId, maybeParentAndSiblings.folder)
      _                 <- validateSiblingNames(folderName, maybeParentAndSiblings)
      _                 <- checkDepth(validatedParentId)
    } yield validatedParentId

    private def getNextRank(siblings: Seq[_]): Int = siblings.length + 1

    private[service] def changeStatusToSharedIfParentIsShared(
        newFolder: api.NewFolder,
        parentFolder: Option[Folder],
        isCloning: Boolean
    ): api.NewFolder = {
      import FolderStatus.SHARED

      parentFolder match {
        case Some(parent) if parent.status == SHARED && !isCloning => newFolder.copy(status = SHARED.toString.some)
        case _                                                     => newFolder
      }
    }

    private def createNewFolder(
        newFolder: api.NewFolder,
        feideId: FeideID,
        makeUniqueNamePostfix: Option[String],
        isCloning: Boolean
    )(implicit
        session: DBSession
    ): Try[domain.Folder] = {

      val parentId      = getMaybeParentId(newFolder.parentId).?
      val maybeSiblings = getFolderWithDirectChildren(parentId, feideId).?
      val nextRank      = getNextRank(maybeSiblings.childrenFolders)
      val withStatus    = changeStatusToSharedIfParentIsShared(newFolder, maybeSiblings.folder, isCloning)
      val folderWithName =
        withStatus.copy(name = getFolderValidName(makeUniqueNamePostfix, newFolder.name, maybeSiblings))
      val validatedParentId = validateNewFolder(folderWithName.name, parentId, maybeSiblings).?
      val newFolderData     = converterService.toNewFolderData(folderWithName, validatedParentId, nextRank.some).?
      val inserted          = folderRepository.insertFolder(feideId, newFolderData).?

      Success(inserted)
    }

    private def getFolderValidName(
        makeUniqueNamePostfix: Option[String],
        folderName: String,
        maybeParentAndSiblings: FolderAndDirectChildren
    ): String = {
      makeUniqueNamePostfix match {
        case None => folderName
        case Some(postfix) =>
          @tailrec
          def getCopyUntilValid(folderName: String): String =
            if (validateSiblingNames(folderName, maybeParentAndSiblings).isFailure) {
              getCopyUntilValid(s"$folderName$postfix")
            } else { folderName }

          getCopyUntilValid(folderName)
      }
    }

    def newFolder(newFolder: api.NewFolder, feideAccessToken: Option[FeideAccessToken]): Try[api.Folder] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId   <- feideApiClient.getFeideID(feideAccessToken)
        _         <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        inserted  <- createNewFolder(newFolder, feideId, makeUniqueNamePostfix = None, isCloning = false)
        crumbs    <- readService.getBreadcrumbs(inserted)(ReadOnlyAutoSession)
        feideUser <- userRepository.userWithFeideId(feideId)
        api       <- converterService.toApiFolder(inserted, crumbs, feideUser)
      } yield api
    }

    private def createOrUpdateFolderResourceConnection(
        folderId: UUID,
        newResource: api.NewResource,
        feideId: FeideID
    )(implicit
        session: DBSession
    ): Try[domain.Resource] =
      for {
        _ <- folderRepository
          .folderWithFeideId(folderId, feideId)
          .orElse(Failure(NotFoundException(s"Can't connect resource to non-existing folder")))
        siblings          <- getFolderWithDirectChildren(folderId.some, feideId)
        insertedOrUpdated <- createNewResourceOrUpdateExisting(newResource, folderId, siblings, feideId)
      } yield insertedOrUpdated

    def newFolderResourceConnection(
        folderId: UUID,
        newResource: api.NewResource,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.Resource] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId   <- feideApiClient.getFeideID(feideAccessToken)
        _         <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        resource  <- createOrUpdateFolderResourceConnection(folderId, newResource, feideId)
        converted <- converterService.toApiResource(resource)
      } yield converted
    }

    private[service] def createNewResourceOrUpdateExisting(
        newResource: api.NewResource,
        folderId: UUID,
        siblings: FolderAndDirectChildren,
        feideId: FeideID
    )(implicit session: DBSession): Try[domain.Resource] = {
      val rank = getNextRank(siblings.childrenResources)
      folderRepository
        .resourceWithPathAndTypeAndFeideId(newResource.path, newResource.resourceType, feideId)
        .flatMap {
          case None =>
            val document = converterService.toDomainResource(newResource)
            for {
              inserted <- folderRepository.insertResource(
                feideId,
                newResource.path,
                newResource.resourceType,
                clock.now(),
                document
              )
              connection <- folderRepository.createFolderResourceConnection(folderId, inserted.id, rank)
            } yield inserted.copy(connection = connection.some)
          case Some(existingResource) =>
            val mergedResource = converterService.mergeResource(existingResource, newResource)
            for {
              updated    <- folderRepository.updateResource(mergedResource)
              connection <- connectIfNotConnected(folderId, mergedResource.id, rank)
            } yield updated.copy(connection = connection.some)
        }
    }

    private def connectIfNotConnected(folderId: UUID, resourceId: UUID, rank: Int)(implicit
        session: DBSession
    ): Try[FolderResource] =
      folderRepository.getConnection(folderId, resourceId) match {
        case Success(Some(connection)) => Success(connection)
        case Success(None)             => folderRepository.createFolderResourceConnection(folderId, resourceId, rank)
        case Failure(ex)               => Failure(ex)
      }

    def updateFolder(
        id: UUID,
        updatedFolder: UpdatedFolder,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.Folder] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId        <- feideApiClient.getFeideID(feideAccessToken)
        _              <- isOperationAllowedOrAccessDenied(feideId, feideAccessToken, updatedFolder)
        existingFolder <- folderRepository.folderWithId(id)
        _              <- existingFolder.isOwner(feideId)
        converted      <- Try(converterService.mergeFolder(existingFolder, updatedFolder))
        maybeSiblings  <- getFolderWithDirectChildren(converted.parentId, feideId)
        _              <- validateUpdatedFolder(converted.name, converted.parentId, maybeSiblings, converted)
        updated        <- folderRepository.updateFolder(id, feideId, converted)
        crumbs         <- readService.getBreadcrumbs(updated)(ReadOnlyAutoSession)
        feideUser      <- userRepository.userWithFeideId(feideId)
        api            <- converterService.toApiFolder(updated, crumbs, feideUser)
      } yield api
    }

    def updateResource(
        id: UUID,
        updatedResource: UpdatedResource,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[api.Resource] = {
      for {
        feideId          <- feideApiClient.getFeideID(feideAccessToken)
        _                <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        existingResource <- folderRepository.resourceWithId(id)
        _                <- existingResource.isOwner(feideId)
        converted = converterService.mergeResource(existingResource, updatedResource)
        updated <- folderRepository.updateResource(converted)
        api     <- converterService.toApiResource(updated)
      } yield api
    }

    private def deleteResourceIfNoConnection(folderId: UUID, resourceId: UUID)(implicit
        session: DBSession
    ): Try[UUID] = {
      folderRepository.folderResourceConnectionCount(resourceId) match {
        case Failure(exception)           => Failure(exception)
        case Success(count) if count == 1 => folderRepository.deleteResource(resourceId)
        case Success(_)                   => folderRepository.deleteFolderResourceConnection(folderId, resourceId)
      }
    }

    private def deleteRecursively(folder: domain.Folder, feideId: FeideID)(implicit session: DBSession): Try[UUID] = {
      for {
        _ <- folder.resources.traverse(res => deleteResourceIfNoConnection(folder.id, res.id))
        _ <- folder.subfolders.traverse(childFolder => deleteRecursively(childFolder, feideId))
        _ <- folderRepository.deleteFolder(folder.id)
      } yield folder.id
    }

    def deleteFolder(id: UUID, feideAccessToken: Option[FeideAccessToken]): Try[UUID] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId         <- feideApiClient.getFeideID(feideAccessToken)
        _               <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        folder          <- folderRepository.folderWithId(id)
        _               <- folder.isOwner(feideId)
        parent          <- getFolderWithDirectChildren(folder.parentId, feideId)
        folderWithData  <- readService.getSingleFolderWithContent(id, includeSubfolders = true, includeResources = true)
        deletedFolderId <- deleteRecursively(folderWithData, feideId)
        siblingsToSort = parent.childrenFolders.filterNot(_.id == deletedFolderId)
        sortRequest    = FolderSortRequest(sortedIds = siblingsToSort.map(_.id))
        _ <- performSort(siblingsToSort, sortRequest, feideId)
      } yield deletedFolderId
    }

    def deleteConnection(
        folderId: UUID,
        resourceId: UUID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[UUID] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId  <- feideApiClient.getFeideID(feideAccessToken)
        _        <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        folder   <- folderRepository.folderWithId(folderId)
        _        <- folder.isOwner(feideId)
        resource <- folderRepository.resourceWithId(resourceId)
        _        <- resource.isOwner(feideId)
        id       <- deleteResourceIfNoConnection(folderId, resourceId)
        parent   <- getFolderWithDirectChildren(folder.id.some, feideId)
        siblingsToSort = parent.childrenResources.filterNot(c => c.resourceId == resourceId && c.folderId == folderId)
        sortRequest    = FolderSortRequest(sortedIds = siblingsToSort.map(_.resourceId))
        _ <- performSort(siblingsToSort, sortRequest, feideId)
      } yield id
    }

    def deleteAllUserData(feideAccessToken: Option[FeideAccessToken]): Try[Unit] = {
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        _       <- folderRepository.deleteAllUserFolders(feideId)
        _       <- folderRepository.deleteAllUserResources(feideId)
        _       <- userRepository.deleteUser(feideId)
      } yield ()
    }

    private[service] def getMyNDLAUserOrFail(feideId: FeideID): Try[domain.MyNDLAUser] = {
      userRepository.userWithFeideId(feideId) match {
        case Failure(ex)         => Failure(ex)
        case Success(None)       => Failure(NotFoundException(s"User with feide_id $feideId was not found"))
        case Success(Some(user)) => Success(user)
      }
    }

    def updateMyNDLAUserData(
        updatedUser: api.UpdatedMyNDLAUser,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.MyNDLAUser] = {
      feideApiClient
        .getFeideID(feideAccessToken)
        .flatMap(feideId => updateFeideUserDataAuthenticated(updatedUser, feideId, feideAccessToken)(AutoSession))
    }

    def adminUpdateMyNDLAUserData(
        updatedUser: api.UpdatedMyNDLAUser,
        feideId: Option[String],
        user: TokenUser
    ): Try[api.MyNDLAUser] = {
      feideId match {
        case None => Failure(ValidationException("feideId", "You need to supply a feideId to update a user."))
        case Some(id) =>
          for {
            existing <- getMyNDLAUserOrFail(id)
            converted = converterService.mergeUserData(existing, updatedUser, Some(user))
            updated     <- userRepository.updateUser(id, converted)
            enabledOrgs <- readService.getMyNDLAEnabledOrgs
            api = converterService.toApiUserData(updated, enabledOrgs)
          } yield api
      }
    }

    private def updateFeideUserDataAuthenticated(
        updatedUser: api.UpdatedMyNDLAUser,
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    )(implicit session: DBSession): Try[api.MyNDLAUser] = {
      for {
        _                <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        existingUserData <- getMyNDLAUserOrFail(feideId)
        combined = converterService.mergeUserData(existingUserData, updatedUser, None)
        updated     <- userRepository.updateUser(feideId, combined)
        enabledOrgs <- readService.getMyNDLAEnabledOrgs
        api = converterService.toApiUserData(updated, enabledOrgs)
      } yield api
    }

    private def performSort(
        rankables: Seq[Rankable],
        sortRequest: FolderSortRequest,
        feideId: FeideID
    ): Try[Unit] = {
      val allIds     = rankables.map(_.sortId)
      val hasEveryId = allIds.forall(sortRequest.sortedIds.contains)
      if (!hasEveryId || allIds.size != sortRequest.sortedIds.size)
        return Failure(
          ValidationException(
            "ids",
            s"You need to supply _every_ direct child of the folder when sorting."
          )
        )

      folderRepository.withTx { session =>
        sortRequest.sortedIds
          .mapWithIndex((id, idx) => {
            val newRank = idx + 1
            val found   = rankables.find(_.sortId == id)
            found match {
              case Some(Folder(folderId, _, _, _, _, _, _, _, _, _, _, _)) =>
                folderRepository.setFolderRank(folderId, newRank, feideId)(session)
              case Some(FolderResource(folderId, resourceId, _)) =>
                folderRepository.setResourceConnectionRank(folderId, resourceId, newRank)(session)
              case _ => Failure(FolderSortException("Something went wrong when sorting! This seems like a bug!"))
            }
          })
          .sequence
          .map(_ => ())
      }
    }

    private def sortRootFolders(sortRequest: FolderSortRequest, feideId: FeideID): Try[Unit] = {
      val session = folderRepository.getSession(true)
      folderRepository
        .foldersWithFeideAndParentID(None, feideId)(session)
        .flatMap(rootFolders => performSort(rootFolders, sortRequest, feideId))
    }

    private def sortNonRootFolderResources(
        folderId: UUID,
        sortRequest: FolderSortRequest,
        feideId: FeideID
    )(implicit
        session: DBSession
    ): Try[Unit] = getFolderWithDirectChildren(folderId.some, feideId).flatMap {
      case FolderAndDirectChildren(_, _, resources) => performSort(resources, sortRequest, feideId)
    }

    private def sortNonRootFolderSubfolders(
        folderId: UUID,
        sortRequest: FolderSortRequest,
        feideId: FeideID
    )(implicit
        session: DBSession
    ): Try[Unit] = getFolderWithDirectChildren(folderId.some, feideId).flatMap {
      case FolderAndDirectChildren(_, subfolders, _) => performSort(subfolders, sortRequest, feideId)
    }

    def sortFolder(
        folderSortObject: FolderSortObject,
        sortRequest: FolderSortRequest,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[Unit] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      val feideId                     = feideApiClient.getFeideID(feideAccessToken).?
      canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken).??
      folderSortObject match {
        case ResourceSorting(parentId) => sortNonRootFolderResources(parentId, sortRequest, feideId)
        case FolderSorting(parentId)   => sortNonRootFolderSubfolders(parentId, sortRequest, feideId)
        case RootFolderSorting()       => sortRootFolders(sortRequest, feideId)
      }
    }

    def changeStatusOfFolderAndItsSubfolders(
        folderId: UUID,
        newStatus: domain.FolderStatus.Value,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[List[UUID]] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId    <- feideApiClient.getFeideID(feideAccessToken)
        _          <- isTeacherOrAccessDenied(feideId, feideAccessToken)
        folder     <- folderRepository.folderWithId(folderId)
        _          <- folder.isOwner(feideId)
        ids        <- folderRepository.getFoldersAndSubfoldersIds(folderId)
        updatedIds <- folderRepository.updateFolderStatusInBulk(ids, newStatus)
      } yield updatedIds
    }

    private[service] def cloneChildrenRecursively(
        sourceFolder: CopyableFolder,
        destinationFolder: domain.Folder,
        feideId: FeideID
    )(implicit session: DBSession): Try[Folder] = {

      val clonedResources = sourceFolder.resources.traverse(res => {
        val newResource =
          api.NewResource(
            resourceType = res.resourceType,
            path = res.path,
            tags = res.tags.some,
            resourceId = res.resourceId
          )
        createOrUpdateFolderResourceConnection(destinationFolder.id, newResource, feideId)
      })

      val clonedSubfolders = sourceFolder.subfolders.traverse(childFolder => {
        val newFolder = domain.NewFolderData(
          parentId = destinationFolder.id.some,
          name = childFolder.name,
          status = FolderStatus.PRIVATE,
          rank = childFolder.rank,
          description = childFolder.description
        )
        folderRepository
          .insertFolder(feideId, newFolder)
          .flatMap(newFolder => cloneChildrenRecursively(childFolder, newFolder, feideId))
      })

      for {
        resources <- clonedResources
        folders   <- clonedSubfolders
      } yield destinationFolder.copy(subfolders = folders, resources = resources)
    }

    private def cloneRecursively(
        sourceFolder: CopyableFolder,
        destinationId: Option[UUID],
        feideId: FeideID,
        makeUniqueRootNamesWithPostfix: Option[String]
    )(implicit
        session: DBSession
    ): Try[domain.Folder] = {
      val sourceFolderCopy = api.NewFolder(
        name = sourceFolder.name,
        parentId = None,
        status = FolderStatus.PRIVATE.toString.some,
        description = sourceFolder.description
      )

      destinationId match {
        case None =>
          for {
            createdFolder <- createNewFolder(
              sourceFolderCopy,
              feideId,
              makeUniqueRootNamesWithPostfix,
              isCloning = true
            )
            clonedFolder <- cloneChildrenRecursively(sourceFolder, createdFolder, feideId)
          } yield clonedFolder
        case Some(id) =>
          for {
            existingFolder <- folderRepository.folderWithId(id)
            clonedSourceFolder = sourceFolderCopy.copy(parentId = existingFolder.id.toString.some)
            createdFolder <- createNewFolder(
              clonedSourceFolder,
              feideId,
              makeUniqueRootNamesWithPostfix,
              isCloning = true
            )
            clonedFolder <- cloneChildrenRecursively(sourceFolder, createdFolder, feideId)
          } yield clonedFolder
      }
    }

    def cloneFolder(
        sourceId: UUID,
        destinationId: Option[UUID],
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.Folder] = {
      folderRepository.rollbackOnFailure { implicit session =>
        for {
          feideId <- feideApiClient.getFeideID(feideAccessToken)
          _       <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
          maybeFolder = folderRepository.getFolderAndChildrenSubfoldersWithResources(sourceId, FolderStatus.SHARED)
          sourceFolder <- readService.getWith404IfNone(sourceId, maybeFolder)
          _            <- sourceFolder.isClonable
          clonedFolder <- cloneRecursively(sourceFolder, destinationId, feideId, "_Kopi".some)(session)
          breadcrumbs  <- readService.getBreadcrumbs(clonedFolder)
          feideUser    <- userRepository.userWithFeideId(feideId)
          converted    <- converterService.toApiFolder(clonedFolder, breadcrumbs, feideUser)
        } yield converted
      }
    }

    private def importFolders(toImport: Seq[api.Folder], feideId: FeideID)(implicit
        session: DBSession
    ): Try[Seq[domain.Folder]] =
      toImport.traverse(folder =>
        cloneRecursively(folder, None, feideId, makeUniqueRootNamesWithPostfix = " (Fra import)".some)
      )

    private def importUser(userData: api.MyNDLAUser, feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(
        implicit session: DBSession
    ): Try[api.MyNDLAUser] =
      for {
        existingUser <- readService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(session)
        newFavorites = (existingUser.favoriteSubjects ++ userData.favoriteSubjects).distinct
        updatedFeideUser = api.UpdatedMyNDLAUser(
          favoriteSubjects = Some(newFavorites),
          arenaEnabled = None,
          shareName = Some(existingUser.shareName)
        )
        updated <- updateFeideUserDataAuthenticated(updatedFeideUser, feideId, feideAccessToken)(session)
      } yield updated

    private def importUserDataAuthenticated(
        toImport: ExportedUserData,
        feideId: FeideID,
        maybeFeideToken: Option[FeideAccessToken]
    ): Try[ExportedUserData] = {
      folderRepository.rollbackOnFailure { session =>
        for {
          _ <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, maybeFeideToken)
          _ <- importUser(toImport.userData, feideId, maybeFeideToken)(session)
          _ <- importFolders(toImport.folders, feideId)(session)
        } yield toImport
      }
    }

    def importUserData(toImport: ExportedUserData, maybeFeideToken: Option[FeideAccessToken]): Try[ExportedUserData] = {
      feideApiClient
        .getFeideID(maybeFeideToken)
        .flatMap(feideId => importUserDataAuthenticated(toImport, feideId, maybeFeideToken))
    }
  }
}

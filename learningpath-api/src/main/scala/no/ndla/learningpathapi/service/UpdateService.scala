/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.{FeideApiClient, SearchApiClient, TaxonomyApiClient}
import no.ndla.learningpathapi.model.api.config.UpdateConfigValue
import no.ndla.learningpathapi.model.api.{config, _}
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import no.ndla.learningpathapi.model.domain.{LearningPathStatus, UserInfo, LearningPath => _, LearningStep => _, _}
import no.ndla.learningpathapi.repository.{ConfigRepository, FolderRepository, LearningPathRepositoryComponent}
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}
import cats.implicits._
import scalikejdbc.{DBSession, ReadOnlyAutoSession}

import java.util.{Date, UUID}
import scala.util.{Failure, Success, Try}

trait UpdateService {
  this: LearningPathRepositoryComponent
    with ReadService
    with ConfigRepository
    with FolderRepository
    with ConverterService
    with SearchIndexService
    with Clock
    with LearningStepValidator
    with LearningPathValidator
    with TaxonomyApiClient
    with FeideApiClient
    with SearchApiClient
    with Props =>
  val updateService: UpdateService

  class UpdateService {

    def updateTaxonomyForLearningPath(
        pathId: Long,
        createResourceIfMissing: Boolean,
        language: String,
        fallback: Boolean,
        userInfo: UserInfo
    ): Try[LearningPathV2] = {
      writeOrAccessDenied(userInfo.isWriter) {
        readService.withIdAndAccessGranted(pathId, userInfo) match {
          case Failure(ex) => Failure(ex)
          case Success(lp) =>
            taxononyApiClient
              .updateTaxonomyForLearningPath(lp, createResourceIfMissing)
              .flatMap(l => converterService.asApiLearningpathV2(l, language, fallback, userInfo))
        }
      }
    }

    def insertDump(dump: domain.LearningPath): domain.LearningPath = learningPathRepository.insert(dump)

    private[service] def writeDuringWriteRestrictionOrAccessDenied[T](owner: UserInfo)(w: => Try[T]): Try[T] =
      writeOrAccessDenied(
        readService.canWriteNow(owner),
        "You do not have write access while write restriction is active."
      )(w)

    private[service] def writeOrAccessDenied[T](
        willExecute: Boolean,
        reason: String = "You do not have permission to perform this action."
    )(w: => Try[T]): Try[T] =
      if (willExecute) w
      else Failure(AccessDeniedException(reason))

    def newFromExistingV2(id: Long, newLearningPath: NewCopyLearningPathV2, owner: UserInfo): Try[LearningPathV2] =
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

    def addLearningPathV2(newLearningPath: NewLearningPathV2, owner: UserInfo): Try[LearningPathV2] =
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
        owner: UserInfo
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

          updateSearchAndTaxonomy(updatedLearningPath).flatMap(_ =>
            converterService.asApiLearningpathV2(
              updatedLearningPath,
              learningPathToUpdate.language,
              fallback = true,
              owner
            )
          )
      }
    }

    private def updateSearchAndTaxonomy(learningPath: domain.LearningPath) = {
      val sRes = searchIndexService.indexDocument(learningPath)

      if (learningPath.isPublished) {
        searchApiClient.indexLearningPathDocument(learningPath)
      } else {
        deleteIsBasedOnReference(learningPath)
      }

      sRes.flatMap(lp => taxononyApiClient.updateTaxonomyForLearningPath(lp, createResourceIfMissing = false))
    }

    def updateLearningPathStatusV2(
        learningPathId: Long,
        status: LearningPathStatus.Value,
        owner: UserInfo,
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
                case Some(msg) if owner.isAdmin => Some(domain.Message(msg, owner.userId, clock.now()))
                case _                          => valid.message
              }

              val updatedLearningPath = learningPathRepository.update(
                valid.copy(message = newMessage, status = status, lastUpdated = clock.now())
              )

              updateSearchAndTaxonomy(updatedLearningPath)
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
        owner: UserInfo
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

                updateSearchAndTaxonomy(updatedPath)
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
        owner: UserInfo
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
                  learningStepValidator.validate(toUpdate, allowUnknownLanguage = true)

                  val (updatedStep, updatedPath) = inTransaction { implicit session =>
                    val updatedStep =
                      learningPathRepository.updateLearningStep(toUpdate)
                    val pathToUpdate = converterService.insertLearningStep(learningPath, updatedStep, owner)
                    val updatedPath  = learningPathRepository.update(pathToUpdate)

                    (updatedStep, updatedPath)
                  }

                  updateSearchAndTaxonomy(updatedPath)
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
        owner: UserInfo
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

                updateSearchAndTaxonomy(updatedPath).flatMap(_ =>
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

    def updateConfig(configKey: ConfigKey, value: UpdateConfigValue, userInfo: UserInfo): Try[config.ConfigMeta] = {

      writeOrAccessDenied(userInfo.isAdmin, "Only administrators can edit configuration.") {
        ConfigMeta(configKey, value.value, new Date(), userInfo.userId).validate.flatMap(newConfigValue => {
          configRepository.updateConfigParam(newConfigValue).map(converterService.asApiConfig)
        })
      }
    }

    def updateSeqNo(learningPathId: Long, learningStepId: Long, seqNo: Int, owner: UserInfo): Try[LearningStepSeqNo] =
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
                    learningPathRepository.updateLearningStep(learningStep.copy(seqNo = seqNo))
                    toUpdate.foreach(step => {
                      learningPathRepository.updateLearningStep(step.copy(seqNo = addOrSubtract(step.seqNo)))
                    })
                  }

                  Success(LearningStepSeqNo(seqNo))
              }
          }
        }
      }

    def rangeToUpdate(from: Int, to: Int): Range = if (from > to) to until from else from + 1 to to

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

    def optimisticLockRetries[T](n: Int)(fn: => T): T = {
      try {
        fn
      } catch {
        case ole: OptimisticLockException =>
          if (n > 1) optimisticLockRetries(n - 1)(fn) else throw ole
        case t: Throwable => throw t
      }
    }
    private def validateParentId(parentId: UUID, feideId: FeideID): Try[UUID] = {
      folderRepository.folderWithFeideId(parentId, feideId) match {
        case Failure(_: NotFoundException) =>
          val paramName = "parentId"
          Failure(
            new ValidationException(
              errors = List(
                ValidationMessage(
                  paramName,
                  s"Invalid value for $paramName. The UUID specified does not exist or is not writable by you."
                )
              )
            )
          )
        case Failure(ex) => Failure(ex)
        case Success(_)  => Success(parentId)
      }
    }

    def newFolder(newFolder: api.NewFolder, feideAccessToken: Option[FeideAccessToken]): Try[api.Folder] = {
      for {
        feideId           <- feideApiClient.getUserFeideID(feideAccessToken)
        document          <- converterService.toDomainFolderDocument(newFolder)
        parentId          <- newFolder.parentId.traverse(pid => converterService.toUUIDValidated(pid.some, "parentId"))
        validatedParentId <- parentId.traverse(pid => validateParentId(pid, feideId))
        inserted          <- folderRepository.insertFolder(feideId, validatedParentId, document)
        crumbs            <- readService.getBreadcrumbs(inserted)(ReadOnlyAutoSession)
        api               <- converterService.toApiFolder(inserted, crumbs)
      } yield api
    }

    def newFolderResourceConnection(
        folderId: UUID,
        newResource: api.NewResource,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.Resource] =
      for {
        feideId <- feideApiClient.getUserFeideID(feideAccessToken)
        _ <- folderRepository
          .folderWithFeideId(folderId, feideId)
          .orElse(Failure(NotFoundException(s"Can't connect resource to non-existing folder")))
        insertedOrUpdated <- createNewResourceOrUpdateExisting(newResource, folderId, feideId)
        converted         <- converterService.toApiResource(insertedOrUpdated)
      } yield converted

    private[service] def createNewResourceOrUpdateExisting(
        newResource: api.NewResource,
        folderId: UUID,
        feideId: FeideID
    ): Try[domain.Resource] =
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
                clock.nowLocalDateTime(),
                document
              )
              _ <- folderRepository.createFolderResourceConnection(folderId, inserted.id)
            } yield inserted
          case Some(existingResource) =>
            val mergedResource = converterService.mergeResource(existingResource, newResource)
            for {
              updated <- folderRepository.updateResource(mergedResource)
              _       <- connectIfNotConnected(folderId, mergedResource.id)
            } yield updated
        }

    private def connectIfNotConnected(folderId: UUID, resourceId: UUID): Try[Unit] =
      folderRepository.isConnected(folderId, resourceId).map {
        case false => folderRepository.createFolderResourceConnection(folderId, resourceId)
        case true  => Success(())
      }

    def updateFolder(
        id: UUID,
        updatedFolder: UpdatedFolder,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[api.Folder] = {
      for {
        feideId        <- feideApiClient.getUserFeideID(feideAccessToken)
        existingFolder <- folderRepository.folderWithId(id)
        _              <- existingFolder.isOwner(feideId)
        converted = converterService.mergeFolder(existingFolder, updatedFolder)
        updated <- folderRepository.updateFolder(id, feideId, converted)
        crumbs  <- readService.getBreadcrumbs(updated)(ReadOnlyAutoSession)
        api     <- converterService.toApiFolder(updated, crumbs)
      } yield api
    }

    def updateResource(
        id: UUID,
        updatedResource: UpdatedResource,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[api.Resource] = {
      for {
        feideId          <- feideApiClient.getUserFeideID(feideAccessToken)
        existingResource <- folderRepository.resourceWithId(id)
        _                <- existingResource.isOwner(feideId)
        converted = converterService.mergeResource(existingResource, updatedResource)
        updated <- folderRepository.updateResource(converted)
        api     <- converterService.toApiResource(updated)
      } yield api
    }

    private def deleteResourceIfNoConnection(resourceId: UUID)(implicit session: DBSession): Try[_] = {
      folderRepository.folderResourceConnectionCount(resourceId) match {
        case Failure(exception)           => Failure(exception)
        case Success(count) if count == 1 => folderRepository.deleteResource(resourceId)
        // The reason that we can "skip" deleting folder-resource connection here is that the connection will be
        // deleted implicitly when the whole folder is deleted.
        case Success(v) => Success(v)
      }
    }

    def deleteRecursively(folder: domain.Folder, feideId: FeideID)(implicit session: DBSession): Try[UUID] = {
      for {
        _ <- folder.resources.traverse(res => deleteResourceIfNoConnection(res.id))
        _ <- folder.subfolders.traverse(childFolder => deleteRecursively(childFolder, feideId))
        _ <- folderRepository.deleteFolder(folder.id)
      } yield folder.id
    }

    def deleteFolder(id: UUID, feideAccessToken: Option[FeideAccessToken]): Try[UUID] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId        <- feideApiClient.getUserFeideID(feideAccessToken)
        folder         <- folderRepository.folderWithId(id)
        _              <- folder.canDelete(feideId)
        folderWithData <- readService.getSingleFolderWithContent(id, includeSubfolders = true, includeResources = false)
        deleted        <- deleteRecursively(folderWithData, feideId)
      } yield deleted
    }

    def deleteConnection(
        folderId: UUID,
        resourceId: UUID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[UUID] = {
      for {
        feideId  <- feideApiClient.getUserFeideID(feideAccessToken)
        folder   <- folderRepository.folderWithId(folderId)
        _        <- folder.isOwner(feideId)
        resource <- folderRepository.resourceWithId(resourceId)
        _        <- resource.isOwner(feideId)
        id <- folderRepository.folderResourceConnectionCount(resourceId) match {
          case Failure(exception)           => Failure(exception)
          case Success(count) if count == 1 => folderRepository.deleteResource(resourceId)
          case Success(_)                   => folderRepository.deleteFolderResourceConnection(folderId, resourceId)
        }
      } yield id
    }
  }

}

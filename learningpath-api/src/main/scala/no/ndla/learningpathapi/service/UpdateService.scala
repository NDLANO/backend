/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import cats.implicits.toTraverseOps
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.implicits.*
import no.ndla.common.model.domain.learningpath
import no.ndla.common.model.domain.learningpath.StepStatus.DELETED
import no.ndla.common.model.domain.learningpath.{LearningPath, LearningPathStatus, LearningStep, Message, StepStatus}
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.{SearchApiClient, TaxonomyApiClient}
import no.ndla.learningpathapi.model.api.*
import no.ndla.learningpathapi.model.domain.*
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.learningpathapi.repository.LearningPathRepository
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}
import no.ndla.network.model.{CombinedUser, CombinedUserRequired}
import no.ndla.network.tapir.auth.TokenUser

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try, boundary}
import no.ndla.language.Language
import no.ndla.database.DBUtility

class UpdateService(using
    learningPathRepository: LearningPathRepository,
    readService: ReadService,
    converterService: ConverterService,
    searchIndexService: SearchIndexService,
    clock: Clock,
    learningStepValidator: LearningStepValidator,
    learningPathValidator: LearningPathValidator,
    taxonomyApiClient: TaxonomyApiClient,
    searchApiClient: SearchApiClient,
    dBUtility: DBUtility,
    props: Props,
) {

  def updateTaxonomyForLearningPath(
      pathId: Long,
      createResourceIfMissing: Boolean,
      language: String,
      fallback: Boolean,
      userInfo: CombinedUser,
  ): Try[LearningPathV2DTO] = {
    writeOrAccessDenied(userInfo.isWriter) {
      readService.withIdAndAccessGranted(pathId, userInfo) match {
        case Failure(ex) => Failure(ex)
        case Success(lp) => taxonomyApiClient
            .updateTaxonomyForLearningPath(lp, createResourceIfMissing, userInfo.tokenUser)
            .flatMap(l => converterService.asApiLearningpathV2(l, language, fallback, userInfo))
      }
    }
  }

  def insertDump(dump: learningpath.LearningPath): Try[learningpath.LearningPath] = learningPathRepository.insert(dump)

  private[service] def writeOrAccessDenied[T](
      willExecute: Boolean,
      reason: String = "You do not have permission to perform this action.",
  )(w: => Try[T]): Try[T] =
    if (willExecute) w
    else Failure(AccessDeniedException(reason))

  def newFromExistingV2(
      id: Long,
      newLearningPath: NewCopyLearningPathV2DTO,
      owner: CombinedUser,
  ): Try[LearningPathV2DTO] = writeOrAccessDenied(owner.canWrite) {
    learningPathRepository.withId(id).map(_.isOwnerOrPublic(owner)) match {
      case None                    => Failure(NotFoundException("Could not find learningpath to copy."))
      case Some(Failure(ex))       => Failure(ex)
      case Some(Success(existing)) => for {
          toInsert  <- converterService.newFromExistingLearningPath(existing, newLearningPath, owner)
          validated <- learningPathValidator.validate(toInsert, allowUnknownLanguage = true)
          inserted  <- learningPathRepository.insert(validated)
          converted <- converterService.asApiLearningpathV2(inserted, newLearningPath.language, fallback = true, owner)
        } yield converted
    }
  }

  def addLearningPathV2(newLearningPath: NewLearningPathV2DTO, owner: CombinedUser): Try[LearningPathV2DTO] =
    writeOrAccessDenied(owner.canWrite) {
      for {
        learningPath <- converterService.newLearningPath(newLearningPath, owner)
        validated    <- learningPathValidator.validate(learningPath)
        inserted     <- learningPathRepository.insert(validated)
        converted    <- converterService.asApiLearningpathV2(inserted, newLearningPath.language, fallback = true, owner)
      } yield converted
    }

  def updateLearningPathV2(
      id: Long,
      learningPathToUpdate: UpdatedLearningPathV2DTO,
      owner: CombinedUser,
  ): Try[LearningPathV2DTO] = writeOrAccessDenied(owner.canWrite) {
    for {
      existing        <- withId(id).flatMap(_.canEditLearningPath(owner))
      validatedUpdate <- learningPathValidator.validate(learningPathToUpdate, existing)
      mergedPath       = converterService.mergeLearningPaths(existing, validatedUpdate)
      // Imported learningpaths may contain fields with language=unknown.
      // We should still be able to update it, but not add new fields with language=unknown.
      validatedMergedPath <- learningPathValidator.validate(mergedPath, allowUnknownLanguage = true)
      updatedLearningPath <- Try(learningPathRepository.update(validatedMergedPath))
      _                   <- updateSearchAndTaxonomy(updatedLearningPath, owner.tokenUser)
      converted           <- converterService.asApiLearningpathV2(
        updatedLearningPath.withOnlyActiveSteps,
        learningPathToUpdate.language,
        fallback = true,
        owner,
      )
    } yield converted
  }

  def deleteLearningPathLanguage(
      learningPathId: Long,
      language: String,
      owner: CombinedUserRequired,
  ): Try[LearningPathV2DTO] = dBUtility.rollbackOnFailure { implicit session =>
    writeOrAccessDenied(owner.canWrite) {
      for {
        learningPath <- withId(learningPathId).flatMap(_.canEditLearningPath(owner))
        updatedSteps <- learningPath
          .learningsteps
          .filter(_.status == StepStatus.ACTIVE)
          .traverse(step => deleteLanguageFromStep(step, language, learningPath))
        withUpdatedSteps    <- Try(converterService.insertLearningSteps(learningPath, updatedSteps))
        withDeletedLanguage <- converterService.deleteLearningPathLanguage(withUpdatedSteps, language)
        updatedPath         <- Try(learningPathRepository.update(withDeletedLanguage))
        _                   <- updateSearchAndTaxonomy(updatedPath, owner.tokenUser)
        converted           <- converterService.asApiLearningpathV2(
          withDeletedLanguage.withOnlyActiveSteps,
          language = Language.DefaultLanguage,
          fallback = true,
          owner,
        )
      } yield converted
    }
  }

  def deleteLearningStepLanguage(
      learningPathId: Long,
      stepId: Long,
      language: String,
      owner: CombinedUserRequired,
  ): Try[LearningStepV2DTO] = dBUtility.rollbackOnFailure { implicit session =>
    writeOrAccessDenied(owner.canWrite) {
      for {
        learningPath <- withId(learningPathId).flatMap(_.canEditLearningPath(owner))
        learningStep <- learningPathRepository
          .learningStepWithId(learningPathId, stepId)
          .toTry(NotFoundException(s"Could not find learningpath with id '$learningPathId'."))
        updatedStep  <- deleteLanguageFromStep(learningStep, language, learningPath)
        pathToUpdate <- Try(converterService.insertLearningStep(learningPath, updatedStep))
        updatedPath  <- Try(learningPathRepository.update(pathToUpdate))
        _            <- updateSearchAndTaxonomy(updatedPath, owner.tokenUser)
        converted    <- converterService.asApiLearningStepV2(
          updatedStep,
          updatedPath,
          language = Language.DefaultLanguage,
          fallback = true,
          owner,
        )
      } yield converted
    }
  }

  private def deleteLanguageFromStep(
      learningStep: LearningStep,
      language: String,
      learningPath: LearningPath,
  ): Try[LearningStep] = {
    for {
      withDeletedLanguage <- converterService.deleteLearningStepLanguage(learningStep, language)
      validated           <- learningStepValidator.validate(withDeletedLanguage, learningPath, allowUnknownLanguage = true)
    } yield incrementStepRevision(validated)
  }

  private def updateSearchAndTaxonomy(learningPath: LearningPath, user: Option[TokenUser]) = {
    val indexPath = learningPath.withOnlyActiveSteps
    val sRes      = searchIndexService.indexDocument(indexPath)

    if (learningPath.isDeleted) {
      deleteIsBasedOnReference(learningPath): Unit
      searchApiClient.deleteLearningPathDocument(learningPath.id.get, user): Unit
    } else {
      searchApiClient.indexLearningPathDocument(indexPath, user): Unit
    }

    sRes.flatMap(lp => taxonomyApiClient.updateTaxonomyForLearningPath(lp, createResourceIfMissing = false, user))
  }

  def updateLearningPathStatusV2(
      learningPathId: Long,
      status: LearningPathStatus,
      owner: CombinedUserRequired,
      language: String,
      message: Option[String] = None,
  ): Try[LearningPathV2DTO] = writeOrAccessDenied(owner.canWrite) {
    withId(learningPathId, includeDeleted = true)
      .flatMap(_.canSetStatus(status, owner))
      .flatMap { existing =>
        val validatedLearningPath =
          if (status == learningpath.LearningPathStatus.PUBLISHED) existing.validateForPublishing()
          else Success(existing)

        validatedLearningPath.flatMap(valid => {
          val newMessage = message match {
            case Some(msg) if owner.isAdmin => Some(Message(msg, owner.id, clock.now()))
            case _                          => valid.message
          }

          val madeAvailable = valid
            .madeAvailable
            .orElse {
              status match {
                case LearningPathStatus.PUBLISHED | LearningPathStatus.UNLISTED => Some(clock.now())
                case _                                                          => None
              }
            }

          val toUpdateWith =
            valid.copy(message = newMessage, status = status, lastUpdated = clock.now(), madeAvailable = madeAvailable)

          val updatedLearningPath = learningPathRepository.update(toUpdateWith)

          updateSearchAndTaxonomy(updatedLearningPath, owner.tokenUser).flatMap(_ =>
            converterService.asApiLearningpathV2(
              updatedLearningPath.withOnlyActiveSteps,
              language,
              fallback = true,
              owner,
            )
          )

        })
      }
  }

  private[service] def deleteIsBasedOnReference(updatedLearningPath: LearningPath): Unit = {
    learningPathRepository
      .learningPathsWithIsBasedOnRaw(updatedLearningPath.id.get)
      .foreach(lp => {
        learningPathRepository.update(lp.copy(lastUpdated = clock.now(), isBasedOn = None))
      })
  }

  def addLearningStepV2(
      learningPathId: Long,
      newLearningStep: NewLearningStepV2DTO,
      owner: CombinedUserRequired,
  ): Try[LearningStepV2DTO] = writeOrAccessDenied(owner.canWrite) {
    optimisticLockRetries(10) {
      withId(learningPathId).flatMap(_.canEditLearningPath(owner)) match {
        case Failure(ex)           => Failure(ex)
        case Success(learningPath) =>
          val activeLearningPath = learningPath.withOnlyActiveSteps
          val validated          = for {
            newStep   <- converterService.asDomainLearningStep(newLearningStep, activeLearningPath, owner.id)
            validated <- learningStepValidator.validate(newStep, activeLearningPath)
          } yield validated

          validated match {
            case Failure(ex)      => Failure(ex)
            case Success(newStep) =>
              val (insertedStep, updatedPath) = learningPathRepository.inTransaction { implicit session =>
                val insertedStep = newStep.copy(
                  id = Some(learningPathRepository.nextLearningStepId),
                  learningPathId = learningPath.id,
                  revision = Some(1),
                )
                val toUpdate    = converterService.insertLearningStep(learningPath, insertedStep)
                val updatedPath = learningPathRepository.update(toUpdate)

                (insertedStep, updatedPath)
              }

              updateSearchAndTaxonomy(updatedPath, owner.tokenUser).flatMap(_ =>
                converterService.asApiLearningStepV2(
                  insertedStep,
                  updatedPath,
                  newLearningStep.language,
                  fallback = true,
                  owner,
                )
              )
          }
      }
    }
  }

  def updateLearningStepV2(
      learningPathId: Long,
      learningStepId: Long,
      learningStepToUpdate: UpdatedLearningStepV2DTO,
      owner: CombinedUserRequired,
  ): Try[LearningStepV2DTO] = writeOrAccessDenied(owner.canWrite) {
    permitTry {
      boundary {
        withId(learningPathId).flatMap(_.canEditLearningPath(owner)) match {
          case Failure(ex)           => Failure(ex)
          case Success(learningPath) =>
            learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
              case None => Failure(
                  NotFoundException(
                    s"Could not find learningstep with id '$learningStepId' to update with learningpath id '$learningPathId'."
                  )
                )
              case Some(existing) =>
                existing.canEditLearningStep(owner) match {
                  case Failure(ex) => boundary.break(Failure(ex))
                  case _           => // continue
                }
                val validated = for {
                  _         <- validateStepRevision(existing, learningStepToUpdate.revision)
                  toUpdate  <- converterService.mergeLearningSteps(existing, learningStepToUpdate)
                  validated <- learningStepValidator.validate(
                    toUpdate.copy(revision = Some(learningStepToUpdate.revision + 1)),
                    learningPath,
                    allowUnknownLanguage = true,
                  )
                } yield validated

                validated match {
                  case Failure(ex)       => Failure(ex)
                  case Success(toUpdate) =>
                    val updatedPath = learningPathRepository.inTransaction { implicit session =>
                      val pathToUpdate = converterService.insertLearningStep(learningPath, toUpdate)
                      learningPathRepository.update(pathToUpdate)
                    }

                    updateSearchAndTaxonomy(updatedPath, owner.tokenUser).flatMap(_ =>
                      converterService.asApiLearningStepV2(
                        toUpdate,
                        updatedPath,
                        learningStepToUpdate.language,
                        fallback = true,
                        owner,
                      )
                    )
                }

            }
        }
      }
    }
  }

  private def updateWithStepSeqNo(
      learningStepId: Long,
      newStatus: StepStatus,
      learningPath: LearningPath,
      stepToUpdate: LearningStep,
      stepsToChange: Seq[LearningStep],
  ): (LearningPath, LearningStep) = learningPathRepository.inTransaction { implicit session =>
    val (_, maybeUpdatedStep, newLearningSteps) = stepsToChange
      .sortBy(_.seqNo)
      .foldLeft((0, Option.empty[LearningStep], Seq.empty[LearningStep])) { case ((seqNo, foundStep, steps), curr) =>
        val now            = clock.now()
        val isChangedStep  = curr.id.contains(learningStepId)
        val stepWithStatus =
          if (isChangedStep) curr.copy(status = newStatus, lastUpdated = now)
          else curr
        val updatedStep = incrementStepRevision(stepWithStatus.copy(seqNo = seqNo, lastUpdated = now))
        val nextSeqNo   =
          if (updatedStep.status == DELETED) seqNo
          else seqNo + 1
        val updatedMainStep =
          if (isChangedStep) Some(updatedStep)
          else foundStep

        (nextSeqNo, updatedMainStep, steps :+ updatedStep)
      }

    val updatedStep = maybeUpdatedStep.getOrElse(stepToUpdate)
    val lp          = converterService.insertLearningSteps(learningPath, newLearningSteps)
    val updatedPath = learningPathRepository.update(lp)
    (updatedPath, updatedStep)
  }

  def updateLearningStepStatusV2(
      learningPathId: Long,
      learningStepId: Long,
      newStatus: StepStatus,
      owner: CombinedUserRequired,
  ): Try[LearningStepV2DTO] = writeOrAccessDenied(owner.canWrite) {
    boundary {
      withId(learningPathId).flatMap(_.canEditLearningPath(owner)) match {
        case Failure(ex)           => Failure(ex)
        case Success(learningPath) =>
          val stepsToChange = learningPathRepository.learningStepsFor(learningPathId)
          val stepToUpdate  = stepsToChange.find(_.id.contains(learningStepId)) match {
            case Some(ls) if ls.status == DELETED && newStatus == DELETED =>
              val msg = s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
              boundary.break(Failure(NotFoundException(msg)))
            case None =>
              val msg = s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
              boundary.break(Failure(NotFoundException(msg)))
            case Some(ls) => ls
          }

          val (updatedPath, updatedStep) =
            updateWithStepSeqNo(learningStepId, newStatus, learningPath, stepToUpdate, stepsToChange)

          updateSearchAndTaxonomy(updatedPath, owner.tokenUser).flatMap(_ =>
            converterService.asApiLearningStepV2(
              updatedStep,
              updatedPath,
              props.DefaultLanguage,
              fallback = true,
              owner,
            )
          )
      }
    }
  }

  def updateSeqNo(
      learningPathId: Long,
      learningStepId: Long,
      seqNo: Int,
      owner: CombinedUser,
  ): Try[LearningStepSeqNoDTO] = {
    writeOrAccessDenied(owner.canWrite) {
      optimisticLockRetries(10) {
        withId(learningPathId).flatMap(_.canEditLearningPath(owner)) match {
          case Failure(ex)           => Failure(ex)
          case Success(learningPath) =>
            learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
              case None => Failure(
                  NotFoundException(s"LearningStep with id $learningStepId in learningPath $learningPathId not found")
                )
              case Some(learningStep) =>
                val activeLearningPath =
                  learningPath.copy(learningsteps = learningPath.learningsteps.filter(_.status == StepStatus.ACTIVE))
                activeLearningPath.validateSeqNo(seqNo)

                val from     = learningStep.seqNo
                val to       = seqNo
                val toUpdate = activeLearningPath
                  .learningsteps
                  .filter(step => rangeToUpdate(from, to).contains(step.seqNo))

                def addOrSubtract(seqNo: Int): Int =
                  if (from > to) seqNo + 1
                  else seqNo - 1
                val now = clock.now()

                val updatedSteps = learningPath
                  .learningsteps
                  .map { step =>
                    if (step.id == learningStep.id) {
                      incrementStepRevision(step.copy(seqNo = seqNo, lastUpdated = now))
                    } else if (toUpdate.exists(_.id == step.id)) {
                      incrementStepRevision(step.copy(seqNo = addOrSubtract(step.seqNo), lastUpdated = now))
                    } else {
                      step
                    }
                  }

                val _ = learningPathRepository.inTransaction { implicit session =>
                  learningPathRepository.update(learningPath.copy(learningsteps = updatedSteps, lastUpdated = now))
                }

                Success(LearningStepSeqNoDTO(seqNo))
            }
        }
      }
    }
  }

  private def rangeToUpdate(from: Int, to: Int): Range =
    if (from > to) to until from
    else from + 1 to to

  private def withId(learningPathId: Long, includeDeleted: Boolean = false): Try[LearningPath] = {
    val lpOpt =
      if (includeDeleted) learningPathRepository.withIdRaw(learningPathId, includeDeleted = true)
      else learningPathRepository.withIdRaw(learningPathId)

    lpOpt match {
      case Some(learningPath) => Success(learningPath)
      case None               => Failure(NotFoundException(s"Could not find learningpath with id '$learningPathId'."))
    }
  }

  private def currentStepRevision(step: LearningStep): Int = step.revision.getOrElse(1)

  private def incrementStepRevision(step: LearningStep): LearningStep = {
    step.copy(revision = Some(currentStepRevision(step) + 1))
  }

  private def validateStepRevision(existing: LearningStep, expectedRevision: Int): Try[Unit] = {
    val currentRevision = currentStepRevision(existing)
    if (currentRevision == expectedRevision) Success(())
    else {
      val msg =
        s"Conflicting revision is detected for learningStep with id = ${existing.id} and revision = $expectedRevision"
      Failure(OptimisticLockException(msg))
    }
  }

  @tailrec
  private def optimisticLockRetries[T](n: Int)(fn: => T): T = {
    try {
      fn
    } catch {
      case ole: OptimisticLockException =>
        if (n > 1) optimisticLockRetries(n - 1)(fn)
        else throw ole
      case t: Throwable => throw t
    }
  }
}

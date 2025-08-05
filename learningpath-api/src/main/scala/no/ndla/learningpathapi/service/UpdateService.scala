/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

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
import no.ndla.learningpathapi.model.domain.ImplicitLearningPath.ImplicitLearningPathMethods
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}
import no.ndla.network.model.{CombinedUser, CombinedUserRequired}
import no.ndla.network.tapir.auth.TokenUser

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import no.ndla.language.Language
import no.ndla.database.DBUtility

trait UpdateService {
  this: LearningPathRepositoryComponent & ReadService & ConverterService & SearchIndexService & Clock &
    LearningStepValidator & LearningPathValidator & TaxonomyApiClient & SearchApiClient & DBUtility & Props =>
  val updateService: UpdateService

  class UpdateService {

    def updateTaxonomyForLearningPath(
        pathId: Long,
        createResourceIfMissing: Boolean,
        language: String,
        fallback: Boolean,
        userInfo: CombinedUser
    ): Try[LearningPathV2DTO] = {
      writeOrAccessDenied(userInfo.isWriter) {
        readService.withIdAndAccessGranted(pathId, userInfo) match {
          case Failure(ex) => Failure(ex)
          case Success(lp) =>
            taxonomyApiClient
              .updateTaxonomyForLearningPath(lp, createResourceIfMissing, userInfo.tokenUser)
              .flatMap(l => converterService.asApiLearningpathV2(l, language, fallback, userInfo))
        }
      }
    }

    def insertDump(dump: learningpath.LearningPath): learningpath.LearningPath = learningPathRepository.insert(dump)

    private[service] def writeDuringWriteRestrictionOrAccessDenied[T](owner: CombinedUser)(w: => Try[T]): Try[T] = for {
      canWrite <- readService.canWriteNow(owner)
      result   <- writeOrAccessDenied(canWrite, "You do not have write access while write restriction is active.")(w)
    } yield result

    private[service] def writeOrAccessDenied[T](
        willExecute: Boolean,
        reason: String = "You do not have permission to perform this action."
    )(w: => Try[T]): Try[T] =
      if (willExecute) w
      else Failure(AccessDeniedException(reason))

    def newFromExistingV2(
        id: Long,
        newLearningPath: NewCopyLearningPathV2DTO,
        owner: CombinedUser
    ): Try[LearningPathV2DTO] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        learningPathRepository.withId(id).map(_.isOwnerOrPublic(owner)) match {
          case None                    => Failure(NotFoundException("Could not find learningpath to copy."))
          case Some(Failure(ex))       => Failure(ex)
          case Some(Success(existing)) =>
            for {
              toInsert <- converterService.newFromExistingLearningPath(existing, newLearningPath, owner)
              _ = learningPathValidator.validate(toInsert, allowUnknownLanguage = true)
              converted <- converterService.asApiLearningpathV2(
                learningPathRepository.insert(toInsert),
                newLearningPath.language,
                fallback = true,
                owner
              )
            } yield converted
        }
      }

    def addLearningPathV2(newLearningPath: NewLearningPathV2DTO, owner: CombinedUser): Try[LearningPathV2DTO] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        for {
          learningPath <- converterService.newLearningPath(newLearningPath, owner)
          _        = learningPathValidator.validate(learningPath)
          inserted = learningPathRepository.insert(learningPath)
          converted <- converterService.asApiLearningpathV2(inserted, newLearningPath.language, fallback = true, owner)
        } yield converted
      }

    def updateLearningPathV2(
        id: Long,
        learningPathToUpdate: UpdatedLearningPathV2DTO,
        owner: CombinedUser
    ): Try[LearningPathV2DTO] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      learningPathValidator.validate(learningPathToUpdate)

      withId(id).flatMap(_.canEditLearningpath(owner)) match {
        case Failure(ex)       => Failure(ex)
        case Success(existing) =>
          val toUpdate = converterService.mergeLearningPaths(existing, learningPathToUpdate, owner)

          // Imported learningpaths may contain fields with language=unknown.
          // We should still be able to update it, but not add new fields with language=unknown.
          learningPathValidator.validate(toUpdate, allowUnknownLanguage = true)

          val updatedLearningPath = learningPathRepository.update(toUpdate)

          updateSearchAndTaxonomy(updatedLearningPath, owner.tokenUser).flatMap(_ =>
            converterService.asApiLearningpathV2(
              updatedLearningPath,
              learningPathToUpdate.language,
              fallback = true,
              owner
            )
          )
      }
    }

    def deleteLearningStepLanguage(
        learningpathId: Long,
        stepId: Long,
        language: String,
        owner: CombinedUserRequired
    ): Try[LearningStepV2DTO] =
      DBUtil.rollbackOnFailure { implicit session =>
        writeDuringWriteRestrictionOrAccessDenied(owner) {
          for {
            learningPath <- withId(learningpathId).flatMap(_.canEditLearningpath(owner))
            learningStep <- learningPathRepository
              .learningStepWithId(learningpathId, stepId)
              .toTry(NotFoundException(s"Could not find learningpath with id '$learningpathId'."))
            withDeletedLanguage <- converterService.deleteLearningStepLanguage(learningStep, language)
            validated           <- learningStepValidator.validate(withDeletedLanguage, allowUnknownLanguage = true)
            updatedStep         <- Try(learningPathRepository.updateLearningStep(validated))
            pathToUpdate        <- Try(converterService.insertLearningStep(learningPath, updatedStep, owner))
            updatedPath         <- Try(learningPathRepository.update(pathToUpdate))
            _                   <- updateSearchAndTaxonomy(updatedPath, owner.tokenUser)
            converted           <- converterService.asApiLearningStepV2(
              updatedStep,
              updatedPath,
              language = Language.DefaultLanguage,
              fallback = true,
              owner
            )
          } yield converted
        }
      }

    private def updateSearchAndTaxonomy(learningPath: LearningPath, user: Option[TokenUser]) = {
      val sRes = searchIndexService.indexDocument(learningPath)

      if (learningPath.isPublished) {
        searchApiClient.indexLearningPathDocument(learningPath, user): Unit
      } else if (learningPath.isDeleted) {
        deleteIsBasedOnReference(learningPath): Unit
      }

      sRes.flatMap(lp => taxonomyApiClient.updateTaxonomyForLearningPath(lp, createResourceIfMissing = false, user))
    }

    def updateLearningPathStatusV2(
        learningPathId: Long,
        status: LearningPathStatus,
        owner: CombinedUserRequired,
        language: String,
        message: Option[String] = None
    ): Try[LearningPathV2DTO] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
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

              val madeAvailable = valid.madeAvailable.orElse {
                status match {
                  case LearningPathStatus.PUBLISHED | LearningPathStatus.UNLISTED => Some(clock.now())
                  case _                                                          => None
                }
              }

              val toUpdateWith = valid.copy(
                message = newMessage,
                status = status,
                lastUpdated = clock.now(),
                madeAvailable = madeAvailable
              )

              val updatedLearningPath = learningPathRepository.update(toUpdateWith)

              updateSearchAndTaxonomy(updatedLearningPath, owner.tokenUser)
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

    private[service] def deleteIsBasedOnReference(updatedLearningPath: LearningPath): Unit = {
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
        newLearningStep: NewLearningStepV2DTO,
        owner: CombinedUserRequired
    ): Try[LearningStepV2DTO] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      optimisticLockRetries(10) {
        withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
          case Failure(ex)           => Failure(ex)
          case Success(learningPath) =>
            val validated = for {
              newStep   <- converterService.asDomainLearningStep(newLearningStep, learningPath)
              validated <- learningStepValidator.validate(newStep)
            } yield validated

            validated match {
              case Failure(ex)      => Failure(ex)
              case Success(newStep) =>
                val (insertedStep, updatedPath) = inTransaction { implicit session =>
                  val insertedStep =
                    learningPathRepository.insertLearningStep(newStep)
                  val toUpdate    = converterService.insertLearningStep(learningPath, insertedStep, owner)
                  val updatedPath = learningPathRepository.update(toUpdate)

                  (insertedStep, updatedPath)
                }

                updateSearchAndTaxonomy(updatedPath, owner.tokenUser)
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
        learningStepToUpdate: UpdatedLearningStepV2DTO,
        owner: CombinedUserRequired
    ): Try[LearningStepV2DTO] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
        case Failure(ex)           => Failure(ex)
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
                case Failure(ex)       => Failure(ex)
                case Success(toUpdate) =>
                  learningStepValidator.validate(toUpdate, allowUnknownLanguage = true).??

                  val (updatedStep, updatedPath) = inTransaction { implicit session =>
                    val updatedStep =
                      learningPathRepository.updateLearningStep(toUpdate)
                    val pathToUpdate = converterService.insertLearningStep(learningPath, updatedStep, owner)
                    val updatedPath  = learningPathRepository.update(pathToUpdate)

                    (updatedStep, updatedPath)
                  }

                  updateSearchAndTaxonomy(updatedPath, owner.tokenUser)
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

    def updateWithStepSeqNo(
        learningStepId: Long,
        newStatus: StepStatus,
        learningPath: LearningPath,
        stepToUpdate: LearningStep,
        stepsToChange: Seq[LearningStep],
        owner: CombinedUserRequired
    ): (LearningPath, LearningStep) = inTransaction { implicit session =>
      val (_, updatedStep, newLearningSteps) =
        stepsToChange.sortBy(_.seqNo).foldLeft((0, stepToUpdate, Seq.empty[LearningStep])) {
          case ((seqNo, foundStep, steps), curr) =>
            val isChangedStep         = curr.id.contains(learningStepId)
            val (mainStep, stepToAdd) =
              if (isChangedStep) (curr.copy(status = newStatus), curr.copy(status = newStatus))
              else (foundStep, curr)
            val updatedMainStep = mainStep.copy(seqNo = seqNo)
            val updatedSteps    = steps :+ stepToAdd.copy(seqNo = seqNo)
            val nextSeqNo       = if (stepToAdd.status == DELETED) seqNo else seqNo + 1

            (nextSeqNo, updatedMainStep, updatedSteps)
        }

      val updated     = newLearningSteps.map(learningPathRepository.updateLearningStep)
      val lp          = converterService.insertLearningSteps(learningPath, updated, owner)
      val updatedPath = learningPathRepository.update(lp.copy(learningsteps = None))
      (updatedPath, updatedStep)
    }

    def updateLearningStepStatusV2(
        learningPathId: Long,
        learningStepId: Long,
        newStatus: StepStatus,
        owner: CombinedUserRequired
    ): Try[LearningStepV2DTO] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
          case Failure(ex)           => Failure(ex)
          case Success(learningPath) =>
            val stepsToChange = learningPathRepository.learningStepsFor(learningPathId)
            val stepToUpdate  = stepsToChange.find(_.id.contains(learningStepId)) match {
              case Some(ls) if ls.status == DELETED && newStatus == DELETED =>
                val msg = s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
                return Failure(NotFoundException(msg))
              case None =>
                val msg = s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
                return Failure(NotFoundException(msg))
              case Some(ls) => ls
            }

            val (updatedPath, updatedStep) =
              updateWithStepSeqNo(learningStepId, newStatus, learningPath, stepToUpdate, stepsToChange, owner)

            updateSearchAndTaxonomy(updatedPath, owner.tokenUser).flatMap(_ =>
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

    def updateSeqNo(
        learningPathId: Long,
        learningStepId: Long,
        seqNo: Int,
        owner: CombinedUser
    ): Try[LearningStepSeqNoDTO] = {
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        optimisticLockRetries(10) {
          withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
            case Failure(ex)           => Failure(ex)
            case Success(learningPath) =>
              learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
                case None =>
                  Failure(
                    NotFoundException(s"LearningStep with id $learningStepId in learningPath $learningPathId not found")
                  )
                case Some(learningStep) =>
                  learningPath.validateSeqNo(seqNo)

                  val from     = learningStep.seqNo
                  val to       = seqNo
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

                  Success(LearningStepSeqNoDTO(seqNo))
              }
          }
        }
      }
    }

    private def rangeToUpdate(from: Int, to: Int): Range = if (from > to) to until from else from + 1 to to

    private def withId(learningPathId: Long, includeDeleted: Boolean = false): Try[LearningPath] = {
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

  }
}

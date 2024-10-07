/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import cats.implicits._
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.implicits._
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.{SearchApiClient, TaxonomyApiClient}
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathTokenUser
import no.ndla.learningpathapi.model.domain.{LearningPathStatus, LearningPath => _, LearningStep => _, _}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.auth.TokenUser

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait UpdateService {
  this: LearningPathRepositoryComponent
    with ReadService
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

    private[service] def writeDuringWriteRestrictionOrAccessDenied[T](owner: TokenUser)(w: => Try[T]): Try[T] = for {
      canWrite <- readService.canWriteNow(owner)
      result   <- writeOrAccessDenied(canWrite, "You do not have write access while write restriction is active.")(w)
    } yield result

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
      } else if (learningPath.isDeleted) {
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

    def updateSeqNo(
        learningPathId: Long,
        learningStepId: Long,
        seqNo: Int,
        owner: TokenUser
    ): Try[LearningStepSeqNo] = {
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

  }
}

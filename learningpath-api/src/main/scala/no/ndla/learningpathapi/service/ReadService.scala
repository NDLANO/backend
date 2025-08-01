/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import cats.implicits.*
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.common.model.api as commonApi
import no.ndla.common.model.domain.learningpath
import no.ndla.common.model.domain.learningpath.{LearningPath, StepStatus, LearningPathStatus as _}
import no.ndla.learningpathapi.model.api.*
import no.ndla.learningpathapi.model.domain.ImplicitLearningPath.ImplicitLearningPathMethods
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.learningpathapi.model.domain.InvalidLpStatusException
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.model.{CombinedUser, CombinedUserRequired}

import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: LearningPathRepositoryComponent & ConverterService & Clock & MyNDLAApiClient =>
  val readService: ReadService

  class ReadService {

    def tags: List[LearningPathTagsDTO] = {
      learningPathRepository.allPublishedTags.map(tags => LearningPathTagsDTO(tags.tags, tags.language))
    }

    def contributors: List[commonApi.AuthorDTO] = {
      learningPathRepository.allPublishedContributors.map(author => commonApi.AuthorDTO(author.`type`, author.name))
    }

    def withOwnerV2(user: CombinedUserRequired, language: String, fallback: Boolean): List[LearningPathV2DTO] = {
      learningPathRepository
        .withOwner(user.id)
        .flatMap(value => converterService.asApiLearningpathV2(value, language, fallback, user).toOption)
    }

    def withIdV2List(
        ids: Seq[Long],
        language: String,
        fallback: Boolean,
        page: Int,
        pageSize: Int,
        userInfo: CombinedUser
    ): Try[List[LearningPathV2DTO]] = {
      if (ids.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
      else {
        val offset        = (page - 1) * pageSize
        val learningpaths = learningPathRepository.pageWithIds(ids, pageSize, offset)
        learningpaths
          .map(_.isOwnerOrPublic(userInfo))
          .collect { case Success(lp) => converterService.asApiLearningpathV2(lp, language, fallback, userInfo) }
          .sequence
      }
    }

    def withIdV2(
        learningPathId: Long,
        language: String,
        fallback: Boolean,
        user: CombinedUser
    ): Try[LearningPathV2DTO] = {
      withIdAndAccessGranted(learningPathId, user).flatMap(lp =>
        converterService.asApiLearningpathV2(lp, language, fallback, user)
      )
    }

    def statusFor(learningPathId: Long, user: CombinedUser): Try[LearningPathStatusDTO] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatusDTO(lp.status.toString))
    }

    def learningStepStatusForV2(
        learningPathId: Long,
        learningStepId: Long,
        language: String,
        fallback: Boolean,
        user: CombinedUser
    ): Try[LearningStepStatusDTO] = {
      learningstepV2For(learningPathId, learningStepId, language, fallback, user).map(ls =>
        LearningStepStatusDTO(ls.status)
      )
    }

    def learningstepsForWithStatusV2(
        learningPathId: Long,
        status: StepStatus,
        language: String,
        fallback: Boolean,
        user: CombinedUser
    ): Try[LearningStepContainerSummaryDTO] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Success(lp) => converterService.asLearningStepContainerSummary(status, lp, language, fallback)
        case Failure(ex) => Failure(ex)
      }
    }

    def learningstepV2For(
        learningPathId: Long,
        learningStepId: Long,
        language: String,
        fallback: Boolean,
        user: CombinedUser
    ): Try[LearningStepV2DTO] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Success(lp) =>
          learningPathRepository
            .learningStepWithId(learningPathId, learningStepId)
            .map(ls => converterService.asApiLearningStepV2(ls, lp, language, fallback, user)) match {
            case Some(value) => value
            case None        =>
              Failure(
                NotFoundException(
                  s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
                )
              )
          }
        case Failure(ex) => Failure(ex)
      }
    }

    def withIdAndAccessGranted(learningPathId: Long, user: CombinedUser): Try[LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.map(_.isOwnerOrPublic(user)) match {
        case Some(Success(lp)) => Success(lp)
        case Some(Failure(ex)) => Failure(ex)
        case None              => Failure(NotFoundException(s"Could not find learningPath with id $learningPathId"))
      }
    }

    def getLearningPathDomainDump(
        pageNo: Int,
        pageSize: Int,
        onlyIncludePublished: Boolean
    ): LearningPathDomainDumpDTO = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))

      val resultFunc =
        if (onlyIncludePublished) learningPathRepository.getPublishedLearningPathByPage _
        else learningPathRepository.getAllLearningPathsByPage _

      val count =
        if (onlyIncludePublished) learningPathRepository.publishedLearningPathCount
        else learningPathRepository.learningPathCount

      val results = resultFunc(safePageSize, (safePageNo - 1) * safePageSize)

      LearningPathDomainDumpDTO(count, safePageNo, safePageSize, results)
    }

    def learningPathWithStatus(status: String, user: CombinedUser): Try[List[LearningPathV2DTO]] = {
      if (user.isAdmin) {
        learningpath.LearningPathStatus.valueOf(status) match {
          case Some(ps) =>
            Success(
              learningPathRepository
                .learningPathsWithStatus(ps)
                .flatMap(lp => converterService.asApiLearningpathV2(lp, "all", fallback = true, user).toOption)
            )
          case _ => Failure(InvalidLpStatusException(s"Parameter '$status' is not a valid status"))
        }
      } else { Failure(AccessDeniedException("You do not have access to this resource.")) }
    }

    def canWriteNow(userInfo: CombinedUser): Try[Boolean] = {
      myndlaApiClient.isWriteRestricted.map(isRestricted => userInfo.canWriteDuringWriteRestriction || !isRestricted)
    }

  }
}

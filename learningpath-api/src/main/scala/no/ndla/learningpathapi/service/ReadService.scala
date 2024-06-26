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
import no.ndla.learningpathapi.integration.MyNDLAApiClient
import no.ndla.learningpathapi.model.api.*
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathTokenUser
import no.ndla.learningpathapi.model.domain.{InvalidLpStatusException, StepStatus, LearningPathStatus as _}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.auth.TokenUser

import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: LearningPathRepositoryComponent
    with FeideApiClient
    with ConverterService
    with Clock
    with RedisClient
    with MyNDLAApiClient =>
  val readService: ReadService

  class ReadService {

    def tags: List[LearningPathTags] = {
      learningPathRepository.allPublishedTags.map(tags => LearningPathTags(tags.tags, tags.language))
    }

    def contributors: List[commonApi.Author] = {
      learningPathRepository.allPublishedContributors.map(author => commonApi.Author(author.`type`, author.name))
    }

    def withOwnerV2(user: TokenUser): List[LearningPathSummaryV2] = {
      learningPathRepository
        .withOwner(user.id)
        .flatMap(value => converterService.asApiLearningpathSummaryV2(value, user).toOption)
    }

    def withIdV2List(
        ids: Seq[Long],
        language: String,
        fallback: Boolean,
        page: Int,
        pageSize: Int,
        userInfo: TokenUser
    ): Try[Seq[LearningPathV2]] = {
      if (ids.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
      else {
        val offset        = (page - 1) * pageSize
        val learningpaths = learningPathRepository.pageWithIds(ids, pageSize, offset)
        learningpaths.traverse(learningpath =>
          converterService.asApiLearningpathV2(learningpath, language, fallback, userInfo)
        )
      }
    }

    def withIdV2(
        learningPathId: Long,
        language: String,
        fallback: Boolean,
        user: TokenUser
    ): Try[LearningPathV2] = {
      withIdAndAccessGranted(learningPathId, user).flatMap(lp =>
        converterService.asApiLearningpathV2(lp, language, fallback, user)
      )
    }

    def statusFor(learningPathId: Long, user: TokenUser): Try[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningStepStatusForV2(
        learningPathId: Long,
        learningStepId: Long,
        language: String,
        fallback: Boolean,
        user: TokenUser
    ): Try[LearningStepStatus] = {
      learningstepV2For(learningPathId, learningStepId, language, fallback, user).map(ls =>
        LearningStepStatus(ls.status)
      )
    }

    def learningstepsForWithStatusV2(
        learningPathId: Long,
        status: StepStatus,
        language: String,
        fallback: Boolean,
        user: TokenUser
    ): Try[LearningStepContainerSummary] = {
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
        user: TokenUser
    ): Try[LearningStepV2] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Success(lp) =>
          learningPathRepository
            .learningStepWithId(learningPathId, learningStepId)
            .map(ls => converterService.asApiLearningStepV2(ls, lp, language, fallback, user)) match {
            case Some(value) => value
            case None =>
              Failure(
                NotFoundException(
                  s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
                )
              )
          }
        case Failure(ex) => Failure(ex)
      }
    }

    def withIdAndAccessGranted(learningPathId: Long, user: TokenUser): Try[domain.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.map(_.isOwnerOrPublic(user)) match {
        case Some(Success(lp)) => Success(lp)
        case Some(Failure(ex)) => Failure(ex)
        case None              => Failure(NotFoundException(s"Could not find learningPath with id $learningPathId"))
      }
    }

    def getLearningPathDomainDump(pageNo: Int, pageSize: Int, onlyIncludePublished: Boolean): LearningPathDomainDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))

      val resultFunc =
        if (onlyIncludePublished) learningPathRepository.getPublishedLearningPathByPage _
        else learningPathRepository.getAllLearningPathsByPage _

      val count =
        if (onlyIncludePublished) learningPathRepository.publishedLearningPathCount
        else learningPathRepository.learningPathCount

      val results = resultFunc(safePageSize, (safePageNo - 1) * safePageSize)

      LearningPathDomainDump(count, safePageNo, safePageSize, results)
    }

    def learningPathWithStatus(status: String, user: TokenUser): Try[List[LearningPathV2]] = {
      if (user.isAdmin) {
        domain.LearningPathStatus.valueOf(status) match {
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

    def canWriteNow(userInfo: TokenUser): Try[Boolean] = {
      myndlaApiClient.isWriteRestricted.map(isRestricted => userInfo.canWriteDuringWriteRestriction || !isRestricted)
    }

  }
}

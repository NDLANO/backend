/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import cats.effect.IO
import no.ndla.common.Clock
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.draft.DraftStatus._
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.{domain => common}
import no.ndla.draftapi.auth.UserInfo
import no.ndla.draftapi.auth.UserInfo.{DirectPublishRoles, PublishRoles}
import no.ndla.draftapi.integration._
import no.ndla.draftapi.model.api.{ErrorHelpers, NotFoundException}
import no.ndla.draftapi.model.domain.{IgnoreFunction, StateTransition}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service.SideEffect.SideEffect
import no.ndla.draftapi.service.search.ArticleIndexService
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.model.RequestInfo

import scala.collection.mutable
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait StateTransitionRules {
  this: WriteService
    with DraftRepository
    with Clock
    with ArticleApiClient
    with TaxonomyApiClient
    with LearningpathApiClient
    with ConceptApiClient
    with H5PApiClient
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with ErrorHelpers
    with SearchApiClient =>

  object StateTransitionRules {

    // Import implicits to clean up SideEffect creation where we don't need all parameters
    import SideEffect.implicits._

    private[service] val checkIfArticleIsInUse: SideEffect = (article: Draft) =>
      doIfArticleIsNotInUse(article.id.getOrElse(1)) {
        Success(article)
      }

    private[service] val unpublishArticle: SideEffect = (article: Draft) =>
      doIfArticleIsNotInUse(article.id.getOrElse(1)) {
        article.id match {
          case Some(id) =>
            val taxMetadataT = taxonomyApiClient.updateTaxonomyMetadataIfExists(id, visible = false)
            val articleUpdT  = articleApiClient.unpublishArticle(article)
            val failures     = Seq(taxMetadataT, articleUpdT).collectFirst { case Failure(ex) => Failure(ex) }
            failures.getOrElse(articleUpdT)
          case _ => Failure(NotFoundException("This is a bug, article to unpublish has no id."))
        }
      }

    private val validateArticleApiArticle: SideEffect = (draft: Draft, isImported: Boolean) => {
      val validatedArticle = converterService.toArticleApiArticle(draft) match {
        case Failure(ex)      => Failure(ex)
        case Success(article) => articleApiClient.validateArticle(article, isImported)
      }
      validatedArticle.map(_ => draft)
    }

    private def publishArticleSideEffect(useSoftValidation: Boolean = false): SideEffect =
      (article, isImported) =>
        article.id match {
          case Some(id) =>
            val externalIds = draftRepository.getExternalIdsFromId(id)

            val h5pPaths = converterService.getEmbeddedH5PPaths(article)
            h5pApiClient.publishH5Ps(h5pPaths)

            val taxonomyT   = taxonomyApiClient.updateTaxonomyIfExists(id, article)
            val articleUdpT = articleApiClient.updateArticle(id, article, externalIds, isImported, useSoftValidation)
            val failures = Seq(taxonomyT, articleUdpT).collectFirst { case Failure(ex) =>
              Failure(ex)
            }
            failures.getOrElse(articleUdpT)
          case _ => Failure(NotFoundException("This is a bug, article to publish has no id."))
        }

    private val publishArticle            = publishArticleSideEffect()
    private val publishWithSoftValidation = publishArticleSideEffect(true)

    private val articleHasNotBeenPublished: Option[IgnoreFunction] = Some((maybeArticle, transition) => {
      maybeArticle match {
        case None => true
        case Some(art) =>
          val hasBeenPublished          = art.status.current == PUBLISHED || art.status.other.contains(PUBLISHED)
          val isFromPublishedTransition = transition.from == PUBLISHED
          !(hasBeenPublished || isFromPublishedTransition)
      }
    })

    private val articleHasBeenPublished: Option[IgnoreFunction] = Some((maybeArticle, transition) => {
      maybeArticle match {
        case None => false
        case Some(art) =>
          val hasBeenPublished          = art.status.current == PUBLISHED || art.status.other.contains(PUBLISHED)
          val isFromPublishedTransition = transition.from == PUBLISHED
          hasBeenPublished || isFromPublishedTransition
      }
    })

    import StateTransition._

    // format: off
    val StateTransitions: mutable.LinkedHashSet[StateTransition] = mutable.LinkedHashSet(
       PLANNED               -> PLANNED,
       PLANNED               -> IN_PROGRESS,
      (PLANNED               -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       ARCHIVED              -> ARCHIVED,
       ARCHIVED              -> IN_PROGRESS,
       IN_PROGRESS           -> IN_PROGRESS,
      (IN_PROGRESS           -> EXTERNAL_REVIEW)       keepCurrentOnTransition,
      (IN_PROGRESS           -> INTERNAL_REVIEW)       keepCurrentOnTransition,
      (IN_PROGRESS           -> QUALITY_ASSURANCE)     .require(PublishRoles, articleHasBeenPublished) keepStates Set(PUBLISHED),
      (IN_PROGRESS           -> LANGUAGE)              .require(PublishRoles, articleHasBeenPublished) keepStates Set(PUBLISHED),
      (IN_PROGRESS           -> END_CONTROL)           .require(PublishRoles, articleHasBeenPublished) keepStates Set(PUBLISHED) withSideEffect validateArticleApiArticle,
      (IN_PROGRESS           -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation,
      (IN_PROGRESS           -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       EXTERNAL_REVIEW       -> IN_PROGRESS            keepStates Set(PUBLISHED),
      (EXTERNAL_REVIEW       -> EXTERNAL_REVIEW)       keepStates Set(IN_PROGRESS, PUBLISHED),
      (EXTERNAL_REVIEW       -> INTERNAL_REVIEW)       keepStates Set(PUBLISHED) keepCurrentOnTransition,
      (EXTERNAL_REVIEW       -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation,
      (EXTERNAL_REVIEW       -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       INTERNAL_REVIEW       -> IN_PROGRESS,
      (INTERNAL_REVIEW       -> EXTERNAL_REVIEW)       keepStates Set(INTERNAL_REVIEW, PUBLISHED),
       INTERNAL_REVIEW       -> INTERNAL_REVIEW,
      (INTERNAL_REVIEW       -> QUALITY_ASSURANCE)     keepCurrentOnTransition,
      (INTERNAL_REVIEW       -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation,
      (INTERNAL_REVIEW       -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
      (QUALITY_ASSURANCE     -> IN_PROGRESS)           keepStates Set(PUBLISHED),
      (QUALITY_ASSURANCE     -> INTERNAL_REVIEW)       keepStates Set(PUBLISHED),
       QUALITY_ASSURANCE     -> QUALITY_ASSURANCE,
      (QUALITY_ASSURANCE     -> LANGUAGE)              keepStates Set(PUBLISHED) require DirectPublishRoles,
      (QUALITY_ASSURANCE     -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
      (QUALITY_ASSURANCE     -> PUBLISHED)             require PublishRoles withSideEffect publishArticle,
       LANGUAGE              -> IN_PROGRESS,
      (LANGUAGE              -> QUALITY_ASSURANCE)     keepCurrentOnTransition,
       LANGUAGE              -> LANGUAGE,
      (LANGUAGE              -> FOR_APPROVAL)          keepStates Set(PUBLISHED) require DirectPublishRoles,
      (LANGUAGE              -> PUBLISHED)             require PublishRoles withSideEffect publishArticle,
      (LANGUAGE              -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
      (FOR_APPROVAL          -> IN_PROGRESS)           keepStates Set(PUBLISHED),
      (FOR_APPROVAL          -> LANGUAGE)              keepStates Set(PUBLISHED),
       FOR_APPROVAL          -> FOR_APPROVAL,
      (FOR_APPROVAL          -> END_CONTROL)           keepStates Set(PUBLISHED) withSideEffect validateArticleApiArticle,
      (FOR_APPROVAL          -> PUBLISHED)             require PublishRoles withSideEffect publishArticle,
      (FOR_APPROVAL          -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
      (END_CONTROL           -> IN_PROGRESS)           keepStates Set(PUBLISHED),
      (END_CONTROL           -> FOR_APPROVAL)          keepStates Set(PUBLISHED),
      (END_CONTROL           -> END_CONTROL)           withSideEffect validateArticleApiArticle,
      (END_CONTROL           -> PUBLISH_DELAYED)       require DirectPublishRoles withSideEffect validateArticleApiArticle,
      (END_CONTROL           -> PUBLISHED)             require DirectPublishRoles withSideEffect publishArticle,
      (END_CONTROL           -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
      (PUBLISH_DELAYED       -> END_CONTROL)           keepStates Set(PUBLISHED) withSideEffect validateArticleApiArticle,
       PUBLISH_DELAYED       -> PUBLISH_DELAYED,
      (PUBLISH_DELAYED       -> PUBLISHED)             require DirectPublishRoles withSideEffect publishArticle,
      (PUBLISH_DELAYED       -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
      (PUBLISHED             -> IN_PROGRESS)           keepStates Set(PUBLISHED) keepCurrentOnTransition,
      (PUBLISHED             -> UNPUBLISHED)           keepStates Set.empty require DirectPublishRoles withSideEffect unpublishArticle,
      (PUBLISHED             -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED) withSideEffect unpublishArticle,
       UNPUBLISHED           -> UNPUBLISHED,
      (UNPUBLISHED           -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation,
       UNPUBLISHED           -> IN_PROGRESS,
      (UNPUBLISHED           -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
    )
    // format: on

    private def getTransition(
        from: DraftStatus.Value,
        to: DraftStatus.Value,
        user: UserInfo,
        current: Draft
    ): Option[StateTransition] = {
      StateTransitions
        .find(transition => transition.from == from && transition.to == to)
        .filter(_.hasRequiredRoles(user, Some(current)))
    }

    private[service] def doTransitionWithoutSideEffect(
        current: Draft,
        to: DraftStatus.Value,
        user: UserInfo,
        isImported: Boolean
    ): (Try[Draft], Seq[SideEffect]) = {
      getTransition(current.status.current, to, user, current) match {
        case Some(t) =>
          val currentToOther = if (t.addCurrentStateToOthersOnTransition) Set(current.status.current) else Set()
          val containsIllegalStatuses = current.status.other.intersect(t.illegalStatuses)
          if (containsIllegalStatuses.nonEmpty) {
            val illegalStateTransition = IllegalStatusStateTransition(
              s"Cannot go to $to when article contains $containsIllegalStatuses"
            )
            return (Failure(illegalStateTransition), Seq.empty)
          }
          val other     = current.status.other.intersect(t.otherStatesToKeepOnTransition) ++ currentToOther
          val newStatus = common.Status(to, other)
          val newEditorNotes =
            if (current.status.current != to)
              current.notes :+ common.EditorNote(
                "Status endret",
                if (isImported) "System" else user.id,
                newStatus,
                clock.now()
              )
            else current.notes
          val convertedArticle = current.copy(status = newStatus, notes = newEditorNotes)
          (Success(convertedArticle), t.sideEffects)
        case None =>
          val illegalStateTransition = IllegalStatusStateTransition(
            s"Cannot go to $to when article is ${current.status.current}"
          )
          (Failure(illegalStateTransition), Seq.empty)
      }
    }

    def doTransition(
        current: Draft,
        to: DraftStatus.Value,
        user: UserInfo,
        isImported: Boolean
    ): IO[Try[Draft]] = {
      val (convertedArticle, sideEffects) = doTransitionWithoutSideEffect(current, to, user, isImported)
      val requestInfo                     = RequestInfo.fromThreadContext()
      IO {
        requestInfo.setRequestInfo()
        convertedArticle.flatMap(articleBeforeSideEffect => {
          sideEffects
            .foldLeft(Try(articleBeforeSideEffect))((accumulatedArticle, sideEffect) => {
              accumulatedArticle.flatMap(a => sideEffect(a, isImported))
            })
        })
      }
    }

    private[this] def learningPathsUsingArticle(articleId: Long): Seq[LearningPath] = {
      val resources = taxonomyApiClient.queryResource(articleId).getOrElse(List.empty).flatMap(_.paths)
      val topics    = taxonomyApiClient.queryTopic(articleId).getOrElse(List.empty).flatMap(_.paths)
      val plainPaths = List(
        s"/article-iframe/*/$articleId",
        s"/article-iframe/*/$articleId/",
        s"/article-iframe/*/$articleId/\\?*",
        s"/article-iframe/*/$articleId\\?*",
        s"/article/$articleId"
      )

      val paths = resources ++ topics ++ plainPaths

      learningpathApiClient.getLearningpathsWithPaths(paths) match {
        case Success(learningpaths) => learningpaths
        case _                      => Seq.empty
      }
    }

    private def doIfArticleIsNotInUse(articleId: Long)(callback: => Try[Draft]): Try[Draft] =
      (
        searchApiClient.draftsWhereUsed(articleId),
        searchApiClient.publishedWhereUsed(articleId),
        learningPathsUsingArticle(articleId)
      ) match {
        case (Nil, Nil, Nil) => callback
        case (draftsUsingArticle, publishedUsingArticle, pathsUsingArticle) =>
          val learningPathIds = pathsUsingArticle.map(lp => s"${lp.id} (${lp.title.title})")
          val draftsIds       = draftsUsingArticle.map(art => s"${art.id} (${art.title.title})")
          val publishedIds    = publishedUsingArticle.map(art => s"${art.id} (${art.title.title})")
          def errorMessage(ids: Seq[_], msg: String): Option[ValidationMessage] =
            Option.when(ids.nonEmpty)(ValidationMessage("status.current", msg))

          val learningPathMessage = errorMessage(
            learningPathIds,
            s"Learningpath(s) ${learningPathIds.mkString(", ")} contains a learning step that uses this article"
          )
          val draftMessage = errorMessage(
            draftsIds,
            s"Article is in use in these draft(s) ${draftsIds.mkString(", ")}"
          )
          val publishedMessage = errorMessage(
            publishedIds,
            s"Article is in use in these published article(s) ${publishedIds.mkString(", ")}"
          )
          Failure(
            new ValidationException(errors = learningPathMessage.toSeq ++ draftMessage.toSeq ++ publishedMessage.toSeq)
          )
      }

  }
}

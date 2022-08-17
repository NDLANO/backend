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
import no.ndla.common.model.{domain => common}
import no.ndla.common.model.domain.draft.{Draft, ArticleStatus}
import no.ndla.common.model.domain.draft.ArticleStatus._
import no.ndla.draftapi.auth.UserInfo
import no.ndla.draftapi.model.api.{ErrorHelpers, NotFoundException}
import no.ndla.draftapi.auth.UserInfo.{DirectPublishRoles, PublishRoles}
import no.ndla.draftapi.integration.{
  ArticleApiClient,
  ConceptApiClient,
  H5PApiClient,
  LearningPath,
  LearningpathApiClient,
  SearchApiClient,
  TaxonomyApiClient
}
import no.ndla.draftapi.model.domain.{IgnoreFunction, StateTransition}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service.SideEffect.SideEffect
import no.ndla.draftapi.service.search.ArticleIndexService
import no.ndla.draftapi.validation.ContentValidator

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

    private val validateArticleApiArticle: SideEffect = (article: Draft, isImported: Boolean) => {
      val articleApiArticle = converterService.toArticleApiArticle(article)
      articleApiClient.validateArticle(articleApiArticle, isImported).map(_ => article)
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

    import StateTransition._

    // format: off
    val StateTransitions: mutable.LinkedHashSet[StateTransition] = mutable.LinkedHashSet(
      (IMPORTED                      -> DRAFT)                         keepCurrentOnTransition,
       DRAFT                         -> DRAFT,
       DRAFT                         -> PROPOSAL,
      (DRAFT                         -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //DRAFT                         -> AWAITING_ARCHIVING,
      (DRAFT                         -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishWithSoftValidation,
       ARCHIVED                      -> ARCHIVED,
       ARCHIVED                      -> DRAFT,
       AWAITING_ARCHIVING            -> DRAFT,
       AWAITING_ARCHIVING            -> AWAITING_ARCHIVING,
      (AWAITING_ARCHIVING            -> AWAITING_UNPUBLISHING)         require PublishRoles withSideEffect checkIfArticleIsInUse keepCurrentOnTransition,
       AWAITING_ARCHIVING            -> UNPUBLISHED                    keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect unpublishArticle,
      (AWAITING_ARCHIVING            -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishArticle,
       PROPOSAL                      -> DRAFT,
       PROPOSAL                      -> PROPOSAL,
      (PROPOSAL                      -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //PROPOSAL                      -> AWAITING_ARCHIVING,
       PROPOSAL                      -> QUEUED_FOR_LANGUAGE,
      (PROPOSAL                      -> USER_TEST)                     keepCurrentOnTransition,
      (PROPOSAL                      -> QUEUED_FOR_PUBLISHING)         keepStates Set(IMPORTED, USER_TEST, QUALITY_ASSURED, QUALITY_ASSURED_DELAYED, PUBLISHED) withSideEffect validateArticleApiArticle require PublishRoles,
      (PROPOSAL                      -> QUEUED_FOR_PUBLISHING_DELAYED) keepStates Set(IMPORTED, USER_TEST, QUALITY_ASSURED, QUALITY_ASSURED_DELAYED, PUBLISHED) withSideEffect validateArticleApiArticle require PublishRoles,
      (PROPOSAL                      -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishWithSoftValidation,
      (PROPOSAL                      -> AWAITING_QUALITY_ASSURANCE)    keepCurrentOnTransition,
      (PROPOSAL                      -> QUALITY_ASSURED)               keepStates Set(IMPORTED, PROPOSAL, PUBLISHED) withSideEffect validateArticleApiArticle,
       USER_TEST                     -> DRAFT,
       USER_TEST                     -> PROPOSAL,
      (USER_TEST                     -> AWAITING_QUALITY_ASSURANCE)    keepStates Set(IMPORTED, PROPOSAL, PUBLISHED) keepCurrentOnTransition,
      (USER_TEST                     -> QUALITY_ASSURED)               keepStates Set(IMPORTED, PROPOSAL, PUBLISHED) withSideEffect validateArticleApiArticle,
      (USER_TEST                     -> USER_TEST)                     keepStates Set(IMPORTED, PROPOSAL, PUBLISHED),
      (USER_TEST                     -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishWithSoftValidation,
      (USER_TEST                     -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //USER_TEST                     -> AWAITING_ARCHIVING,
       AWAITING_QUALITY_ASSURANCE    -> DRAFT,
       AWAITING_QUALITY_ASSURANCE    -> QUEUED_FOR_LANGUAGE,
      (AWAITING_QUALITY_ASSURANCE    -> USER_TEST)                     keepStates Set(IMPORTED, PROPOSAL, PUBLISHED),
      (AWAITING_QUALITY_ASSURANCE    -> AWAITING_QUALITY_ASSURANCE)    keepStates Set(IMPORTED, PROPOSAL, USER_TEST, PUBLISHED),
      (AWAITING_QUALITY_ASSURANCE    -> QUALITY_ASSURED)               keepStates Set(IMPORTED, USER_TEST, PUBLISHED) withSideEffect validateArticleApiArticle,
      (AWAITING_QUALITY_ASSURANCE    -> QUALITY_ASSURED_DELAYED)       keepStates Set(IMPORTED, USER_TEST, PUBLISHED) withSideEffect validateArticleApiArticle,
      (AWAITING_QUALITY_ASSURANCE    -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishWithSoftValidation,
      (AWAITING_QUALITY_ASSURANCE    -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //AWAITING_QUALITY_ASSURANCE    -> AWAITING_ARCHIVING,
       QUALITY_ASSURED               -> DRAFT,
       QUALITY_ASSURED               -> QUALITY_ASSURED,
      (QUALITY_ASSURED               -> QUEUED_FOR_PUBLISHING)         keepStates Set(IMPORTED, USER_TEST, PUBLISHED) require PublishRoles withSideEffect validateArticleApiArticle keepCurrentOnTransition,
      (QUALITY_ASSURED               -> QUEUED_FOR_PUBLISHING_DELAYED) keepStates Set(IMPORTED, USER_TEST, PUBLISHED) require PublishRoles withSideEffect validateArticleApiArticle keepCurrentOnTransition,
      (QUALITY_ASSURED               -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishArticle,
      (QUALITY_ASSURED               -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //QUALITY_ASSURED               -> AWAITING_ARCHIVING,
       QUALITY_ASSURED_DELAYED       -> DRAFT,
       QUALITY_ASSURED_DELAYED       -> QUALITY_ASSURED_DELAYED,
      (QUALITY_ASSURED_DELAYED       -> QUEUED_FOR_PUBLISHING_DELAYED) keepStates Set(IMPORTED, USER_TEST, PUBLISHED) require PublishRoles withSideEffect validateArticleApiArticle keepCurrentOnTransition,
      (QUALITY_ASSURED_DELAYED       -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //QUALITY_ASSURED_DELAYED       -> AWAITING_ARCHIVING,
       QUEUED_FOR_PUBLISHING         -> DRAFT,
       QUEUED_FOR_PUBLISHING         -> QUEUED_FOR_PUBLISHING          withSideEffect validateArticleApiArticle,
      (QUEUED_FOR_PUBLISHING         -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishArticle,
      (QUEUED_FOR_PUBLISHING         -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //QUEUED_FOR_PUBLISHING         -> AWAITING_ARCHIVING,
       QUEUED_FOR_PUBLISHING_DELAYED -> DRAFT,
       QUEUED_FOR_PUBLISHING_DELAYED -> QUEUED_FOR_PUBLISHING_DELAYED  withSideEffect validateArticleApiArticle,
      (QUEUED_FOR_PUBLISHING_DELAYED -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishArticle,
      (QUEUED_FOR_PUBLISHING_DELAYED -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //QUEUED_FOR_PUBLISHING_DELAYED -> AWAITING_ARCHIVING,
      (PUBLISHED                     -> DRAFT)                         keepCurrentOnTransition,
      (PUBLISHED                     -> PROPOSAL)                      keepCurrentOnTransition,
      (PUBLISHED                     -> AWAITING_UNPUBLISHING)         require PublishRoles withSideEffect checkIfArticleIsInUse keepCurrentOnTransition,
      (PUBLISHED                     -> UNPUBLISHED)                   keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect unpublishArticle,
      (PUBLISHED                     -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED) withSideEffect unpublishArticle,
       //PUBLISHED                     -> AWAITING_ARCHIVING,
       AWAITING_UNPUBLISHING         -> DRAFT,
      (AWAITING_UNPUBLISHING         -> PUBLISHED)                     keepStates Set(IMPORTED) require PublishRoles withSideEffect publishArticle,
      (AWAITING_UNPUBLISHING         -> AWAITING_UNPUBLISHING)         withSideEffect checkIfArticleIsInUse keepCurrentOnTransition,
      (AWAITING_UNPUBLISHING         -> UNPUBLISHED)                   keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect unpublishArticle,
      (AWAITING_UNPUBLISHING         -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //AWAITING_UNPUBLISHING         -> AWAITING_ARCHIVING,
      (UNPUBLISHED                   -> PUBLISHED)                     keepStates Set(IMPORTED) require DirectPublishRoles withSideEffect publishWithSoftValidation,
       UNPUBLISHED                   -> DRAFT,
       UNPUBLISHED                   -> PROPOSAL,
       UNPUBLISHED                   -> UNPUBLISHED,
      (UNPUBLISHED                   -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //UNPUBLISHED                   -> AWAITING_ARCHIVING,
       QUEUED_FOR_LANGUAGE           -> DRAFT,
       QUEUED_FOR_LANGUAGE           -> PROPOSAL,
       QUEUED_FOR_LANGUAGE           -> QUEUED_FOR_LANGUAGE,
       QUEUED_FOR_LANGUAGE           -> TRANSLATED,
      (QUEUED_FOR_LANGUAGE           -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //QUEUED_FOR_LANGUAGE           -> AWAITING_ARCHIVING,
      (QUEUED_FOR_LANGUAGE           -> PUBLISHED)                     keepStates Set(IMPORTED) require PublishRoles withSideEffect publishArticle,
       TRANSLATED                    -> PROPOSAL,
       TRANSLATED                    -> TRANSLATED,
       TRANSLATED                    -> AWAITING_QUALITY_ASSURANCE,
      (TRANSLATED                    -> QUALITY_ASSURED)               keepStates Set(IMPORTED, USER_TEST, PUBLISHED) withSideEffect validateArticleApiArticle,
      (TRANSLATED                    -> PUBLISHED)                     keepStates Set(IMPORTED) require PublishRoles withSideEffect publishArticle,
      (TRANSLATED                    -> ARCHIVED)                     .require(PublishRoles, articleHasNotBeenPublished) illegalStatuses Set(PUBLISHED),
       //TRANSLATED                    -> AWAITING_ARCHIVING,
    )
    // format: on

    private def getTransition(
        from: ArticleStatus.Value,
        to: ArticleStatus.Value,
        user: UserInfo,
        current: Draft
    ): Option[StateTransition] = {
      StateTransitions
        .find(transition => transition.from == from && transition.to == to)
        .filter(_.hasRequiredRoles(user, Some(current)))
    }

    private[service] def doTransitionWithoutSideEffect(
        current: Draft,
        to: ArticleStatus.Value,
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
        to: ArticleStatus.Value,
        user: UserInfo,
        isImported: Boolean
    ): IO[Try[Draft]] = {
      val (convertedArticle, sideEffects) = doTransitionWithoutSideEffect(current, to, user, isImported)
      IO {
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

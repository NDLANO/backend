/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.common.Clock
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.Responsible
import no.ndla.common.model.domain.draft.DraftStatus._
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.{domain => common}
import no.ndla.draftapi.integration._
import no.ndla.draftapi.model.api.{ErrorHelpers, NotFoundException}
import no.ndla.draftapi.model.domain.{IgnoreFunction, StateTransition}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service.SideEffect.SideEffect
import no.ndla.draftapi.service.search.ArticleIndexService
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.tapir.auth.{Permission, TokenUser}
import scalikejdbc.ReadOnlyAutoSession

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
    with H5PApiClient
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with ErrorHelpers
    with SearchApiClient =>

  object StateTransitionRules {

    // Import implicits to clean up SideEffect creation where we don't need all parameters
    import SideEffect.implicits._

    private[service] val checkIfArticleIsInUse: SideEffect = (article: Draft, _, user: TokenUser) =>
      doIfArticleIsNotInUse(article.id.getOrElse(1), user) {
        Success(article)
      }

    private val resetResponsible: SideEffect = (article: Draft) => {
      Success(article.copy(responsible = None))
    }

    private val addResponsible: SideEffect = (article: Draft, _: Boolean, user: TokenUser) => {
      val responsible = article.responsible.getOrElse(Responsible(user.id, clock.now()))
      Success(article.copy(responsible = Some(responsible)))
    }

    private[service] val unpublishArticle: SideEffect = (article: Draft, _, user: TokenUser) =>
      doIfArticleIsNotInUse(article.id.getOrElse(1), user) {
        article.id match {
          case Some(id) =>
            val taxMetadataT = taxonomyApiClient.updateTaxonomyMetadataIfExists(id, visible = false, user)
            val articleUpdT  = articleApiClient.unpublishArticle(article, user)
            val failures     = Seq(taxMetadataT, articleUpdT).collectFirst { case Failure(ex) => Failure(ex) }
            failures.getOrElse(articleUpdT)
          case _ => Failure(NotFoundException("This is a bug, article to unpublish has no id."))
        }
      }

    private val validateArticleApiArticle: SideEffect = (draft: Draft, isImported: Boolean, user: TokenUser) => {
      val validatedArticle = converterService.toArticleApiArticle(draft) match {
        case Failure(ex)      => Failure(ex)
        case Success(article) => articleApiClient.validateArticle(article, isImported, Some(user))
      }
      validatedArticle.map(_ => draft)
    }

    private def publishArticleSideEffect(useSoftValidation: Boolean): SideEffect =
      (article, isImported, user) =>
        article.id match {
          case Some(id) =>
            val externalIds = draftRepository.getExternalIdsFromId(id)(ReadOnlyAutoSession)

            val h5pPaths = converterService.getEmbeddedH5PPaths(article)
            h5pApiClient.publishH5Ps(h5pPaths, user): Unit

            val taxonomyT    = taxonomyApiClient.updateTaxonomyIfExists(id, article, user)
            val taxMetadataT = taxonomyApiClient.updateTaxonomyMetadataIfExists(id, visible = true, user)
            val articleUdpT =
              articleApiClient.updateArticle(id, article, externalIds, isImported, useSoftValidation, user)
            val failures = Seq(taxonomyT, taxMetadataT, articleUdpT).collectFirst { case Failure(ex) =>
              Failure(ex)
            }
            failures.getOrElse(articleUdpT)
          case _ => Failure(NotFoundException("This is a bug, article to publish has no id."))
        }

    private val publishArticle            = publishArticleSideEffect(useSoftValidation = false)
    private val publishWithSoftValidation = publishArticleSideEffect(useSoftValidation = true)

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

    private val PublishRoles: Set[Permission]       = Set(Permission.DRAFT_API_WRITE, Permission.DRAFT_API_PUBLISH)
    private val DirectPublishRoles: Set[Permission] = PublishRoles + Permission.DRAFT_API_ADMIN

    // format: off
    val StateTransitions: mutable.LinkedHashSet[StateTransition] = mutable.LinkedHashSet(
       PLANNED               -> PLANNED,
       PLANNED               -> IN_PROGRESS,
      (PLANNED               -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
       ARCHIVED              -> ARCHIVED               withSideEffect resetResponsible,
       ARCHIVED              -> IN_PROGRESS,
       IN_PROGRESS           -> IN_PROGRESS,
      (IN_PROGRESS           -> EXTERNAL_REVIEW)       keepCurrentOnTransition,
      (IN_PROGRESS           -> INTERNAL_REVIEW)       keepCurrentOnTransition,
      (IN_PROGRESS           -> QUALITY_ASSURANCE)     .require(PublishRoles, articleHasBeenPublished) keepStates Set(PUBLISHED),
      (IN_PROGRESS           -> LANGUAGE)              .require(PublishRoles, articleHasBeenPublished) keepStates Set(PUBLISHED),
      (IN_PROGRESS           -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation withSideEffect resetResponsible,
      (IN_PROGRESS           -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
      (IN_PROGRESS           -> REPUBLISH)             .require(PublishRoles, articleHasBeenPublished) keepStates Set(PUBLISHED) requireStatusesForTransition Set(PUBLISHED),
       EXTERNAL_REVIEW       -> IN_PROGRESS            keepStates Set(PUBLISHED),
      (EXTERNAL_REVIEW       -> EXTERNAL_REVIEW)       keepStates Set(IN_PROGRESS, PUBLISHED),
      (EXTERNAL_REVIEW       -> INTERNAL_REVIEW)       keepStates Set(PUBLISHED) keepCurrentOnTransition,
      (EXTERNAL_REVIEW       -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation withSideEffect resetResponsible,
      (EXTERNAL_REVIEW       -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
       INTERNAL_REVIEW       -> IN_PROGRESS,
      (INTERNAL_REVIEW       -> EXTERNAL_REVIEW)       keepStates Set(INTERNAL_REVIEW, PUBLISHED),
       INTERNAL_REVIEW       -> INTERNAL_REVIEW,
      (INTERNAL_REVIEW       -> QUALITY_ASSURANCE)     keepCurrentOnTransition,
      (INTERNAL_REVIEW       -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation withSideEffect resetResponsible,
      (INTERNAL_REVIEW       -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
      (QUALITY_ASSURANCE     -> IN_PROGRESS)           keepStates Set(PUBLISHED),
      (QUALITY_ASSURANCE     -> INTERNAL_REVIEW)       keepStates Set(PUBLISHED),
       QUALITY_ASSURANCE     -> QUALITY_ASSURANCE,
      (QUALITY_ASSURANCE     -> LANGUAGE)              keepStates Set(PUBLISHED) require DirectPublishRoles,
      (QUALITY_ASSURANCE     -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
      (QUALITY_ASSURANCE     -> PUBLISHED)             require PublishRoles withSideEffect publishArticle withSideEffect resetResponsible,
       LANGUAGE              -> IN_PROGRESS,
      (LANGUAGE              -> QUALITY_ASSURANCE)     keepCurrentOnTransition,
       LANGUAGE              -> LANGUAGE,
      (LANGUAGE              -> FOR_APPROVAL)          keepStates Set(PUBLISHED) require DirectPublishRoles,
      (LANGUAGE              -> PUBLISHED)             require PublishRoles withSideEffect publishArticle withSideEffect resetResponsible,
      (LANGUAGE              -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
      (FOR_APPROVAL          -> IN_PROGRESS)           keepStates Set(PUBLISHED),
      (FOR_APPROVAL          -> LANGUAGE)              keepStates Set(PUBLISHED),
       FOR_APPROVAL          -> FOR_APPROVAL,
      (FOR_APPROVAL          -> END_CONTROL)           keepStates Set(PUBLISHED) withSideEffect validateArticleApiArticle,
      (FOR_APPROVAL          -> PUBLISHED)             require PublishRoles withSideEffect publishArticle withSideEffect resetResponsible,
      (FOR_APPROVAL          -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
      (END_CONTROL           -> IN_PROGRESS)           keepStates Set(PUBLISHED),
      (END_CONTROL           -> FOR_APPROVAL)          keepStates Set(PUBLISHED),
      (END_CONTROL           -> END_CONTROL)           withSideEffect validateArticleApiArticle,
      (END_CONTROL           -> PUBLISH_DELAYED)       require DirectPublishRoles withSideEffect validateArticleApiArticle,
      (END_CONTROL           -> PUBLISHED)             require DirectPublishRoles withSideEffect publishArticle withSideEffect resetResponsible,
      (END_CONTROL           -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
      (PUBLISH_DELAYED       -> END_CONTROL)           keepStates Set(PUBLISHED) withSideEffect validateArticleApiArticle,
       PUBLISH_DELAYED       -> PUBLISH_DELAYED,
      (PUBLISH_DELAYED       -> PUBLISHED)             require DirectPublishRoles withSideEffect publishArticle withSideEffect resetResponsible,
      (PUBLISH_DELAYED       -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
      (PUBLISHED             -> IN_PROGRESS)           keepStates Set(PUBLISHED) withSideEffect addResponsible keepCurrentOnTransition,
      (PUBLISHED             -> UNPUBLISHED)           keepStates Set.empty require DirectPublishRoles withSideEffect unpublishArticle withSideEffect resetResponsible,
      (PUBLISHED             -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect unpublishArticle withSideEffect resetResponsible,
      REPUBLISH              -> REPUBLISH,
      REPUBLISH              -> IN_PROGRESS            keepStates Set(PUBLISHED),
      (REPUBLISH             -> PUBLISHED)             require PublishRoles withSideEffect publishArticle withSideEffect resetResponsible,
      UNPUBLISHED            -> UNPUBLISHED            withSideEffect resetResponsible,
      (UNPUBLISHED           -> PUBLISHED)             require DirectPublishRoles withSideEffect publishWithSoftValidation withSideEffect resetResponsible,
       UNPUBLISHED           -> IN_PROGRESS,
      (UNPUBLISHED           -> ARCHIVED)              .require(PublishRoles, articleHasNotBeenPublished) withIllegalStatuses Set(PUBLISHED) withSideEffect resetResponsible,
    )
    // format: on

    private def getTransition(
        from: DraftStatus,
        to: DraftStatus,
        user: TokenUser,
        current: Draft
    ): Option[StateTransition] = {
      StateTransitions
        .find(transition => transition.from == from && transition.to == to)
        .filter(_.hasRequiredProperties(user, Some(current)))
    }

    private def validateTransition(draft: Draft, transition: StateTransition): Try[Unit] = {
      val statusRequiresResponsible = DraftStatus.thatRequiresResponsible.contains(transition.to)
      val statusFromPublishedToInProgress =
        draft.status.current == PUBLISHED && transition.to == IN_PROGRESS
      if (statusRequiresResponsible && draft.responsible.isEmpty && !statusFromPublishedToInProgress) {
        return Failure(
          IllegalStatusStateTransition(
            s"The action triggered a state transition to ${transition.to}, this is invalid without setting new responsible."
          )
        )
      }

      val containsIllegalStatuses = draft.status.other.intersect(transition.illegalStatuses)
      if (containsIllegalStatuses.nonEmpty) {
        val illegalStateTransition = IllegalStatusStateTransition(
          s"Cannot go to ${transition.to} when article contains $containsIllegalStatuses"
        )
        return Failure(illegalStateTransition)
      }

      Success(())
    }

    private def newEditorNotesForTransition(
        current: Draft,
        to: DraftStatus,
        newStatus: common.Status,
        user: TokenUser,
        isImported: Boolean
    ) = {
      if (current.status.current != to)
        current.notes :+ common.EditorNote(
          "Status endret",
          if (isImported) "System" else user.id,
          newStatus,
          clock.now()
        )
      else current.notes
    }

    private[service] def doTransitionWithoutSideEffect(
        current: Draft,
        to: DraftStatus,
        user: TokenUser,
        isImported: Boolean
    ): (Try[Draft], Seq[SideEffect]) = {
      getTransition(current.status.current, to, user, current) match {
        case Some(t) =>
          validateTransition(current, t) match {
            case Failure(ex) => (Failure(ex), Seq.empty)
            case Success(_) =>
              val currentToOther   = if (t.addCurrentStateToOthersOnTransition) Set(current.status.current) else Set()
              val other            = current.status.other.intersect(t.otherStatesToKeepOnTransition) ++ currentToOther
              val newStatus        = common.Status(to, other)
              val newEditorNotes   = newEditorNotesForTransition(current, to, newStatus, user, isImported)
              val convertedArticle = current.copy(status = newStatus, notes = newEditorNotes)

              (Success(convertedArticle), t.sideEffects)
          }
        case None =>
          val illegalStateTransition = IllegalStatusStateTransition(
            s"Cannot go to $to when article is ${current.status.current}"
          )
          (Failure(illegalStateTransition), Seq.empty)
      }
    }

    def doTransition(
        current: Draft,
        to: DraftStatus,
        user: TokenUser,
        isImported: Boolean
    ): Try[Draft] = {
      println("------------")
      val (convertedArticle, sideEffects) = doTransitionWithoutSideEffect(current, to, user, isImported)
      val x = convertedArticle.flatMap(articleBeforeSideEffect => {
        println("We in the flatmap converting article")
        var idx = 0
        sideEffects
          .foldLeft(Try(articleBeforeSideEffect))((accumulatedArticle, sideEffect) => {
            if (accumulatedArticle == null) {
              println(s"Null article on sideEffect ${idx}")
            }
            idx += 1
            accumulatedArticle.flatMap(a => {
              val result = sideEffect(a, isImported, user)
              println("sideeffect returned: " + result)
              result
            })
          })
      })
      println("------------")
      x
    }

    private[this] def learningPathsUsingArticle(articleId: Long, user: TokenUser): Seq[LearningPath] = {
      learningpathApiClient.getLearningpathsWithId(articleId, user) match {
        case Success(learningpaths) => learningpaths
        case _                      => Seq.empty
      }
    }

    private def doIfArticleIsNotInUse(articleId: Long, user: TokenUser)(callback: => Try[Draft]): Try[Draft] =
      (
        searchApiClient.publishedWhereUsed(articleId, user),
        learningPathsUsingArticle(articleId, user)
      ) match {
        case (Nil, Nil) => callback
        case (publishedUsingArticle, pathsUsingArticle) =>
          val learningPathIds = pathsUsingArticle.map(lp => s"${lp.id} (${lp.title.title})")
          val publishedIds    = publishedUsingArticle.map(art => s"${art.id} (${art.title.title})")
          def errorMessage(ids: Seq[_], msg: String): Option[ValidationMessage] =
            Option.when(ids.nonEmpty)(ValidationMessage("status.current", msg))

          val learningPathMessage = errorMessage(
            learningPathIds,
            s"Learningpath(s) ${learningPathIds.mkString(", ")} contains a learning step that uses this article"
          )
          val publishedMessage = errorMessage(
            publishedIds,
            s"Article is in use in these published article(s) ${publishedIds.mkString(", ")}"
          )
          Failure(new ValidationException(errors = learningPathMessage.toSeq ++ publishedMessage.toSeq))
      }

  }
}

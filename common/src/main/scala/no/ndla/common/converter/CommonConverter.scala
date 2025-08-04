/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.converter

import no.ndla.common.model.api.{CommentDTO, NewCommentDTO, UpdatedCommentDTO}
import no.ndla.common.model.domain.Comment
import no.ndla.common.{Clock, UUIDUtil}

trait CommonConverter {
  this: Clock & UUIDUtil =>

  object CommonConverter {
    def newCommentApiToDomain(comment: NewCommentDTO): Comment = {
      Comment(
        id = uuidUtil.randomUUID(),
        created = clock.now(),
        updated = clock.now(),
        content = comment.content,
        isOpen = comment.isOpen.getOrElse(true),
        solved = false
      )
    }
    def commentDomainToApi(comment: Comment): CommentDTO = {
      CommentDTO(
        id = comment.id.toString,
        content = comment.content,
        created = comment.created,
        updated = comment.updated,
        isOpen = comment.isOpen,
        solved = comment.solved
      )
    }
    def mergeUpdatedCommentsWithExisting(
        updatedComments: List[UpdatedCommentDTO],
        existingComments: Seq[Comment]
    ): Seq[Comment] = {
      updatedComments.map(updatedComment => {
        existingComments.find(cc => updatedComment.id.contains(cc.id.toString)) match {
          case Some(existingComment) =>
            val isContentChanged = updatedComment.content != existingComment.content
            val newUpdated       = if (isContentChanged) clock.now() else existingComment.updated
            existingComment.copy(
              updated = newUpdated,
              content = updatedComment.content,
              isOpen = updatedComment.isOpen.getOrElse(true),
              solved = updatedComment.solved.getOrElse(false)
            )
          case None =>
            Comment(
              id = uuidUtil.randomUUID(),
              created = clock.now(),
              updated = clock.now(),
              content = updatedComment.content,
              isOpen = updatedComment.isOpen.getOrElse(true),
              solved = updatedComment.solved.getOrElse(false)
            )
        }
      })
    }
  }
}

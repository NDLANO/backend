/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.converter

import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{NewCommentDTO, UpdatedCommentDTO}
import no.ndla.common.model.domain.Comment
import no.ndla.testbase.UnitTestSuiteBase
import org.mockito.Mockito.when

import java.util.UUID

class CommonConverterTest extends UnitTestSuiteBase with CommonConverter with Clock with UUIDUtil {
  override lazy val clock: SystemClock = mock[SystemClock]
  override lazy val uuidUtil: UUIDUtil = mock[UUIDUtil]
  test("that mergeUpdatedCommentsWithExisting creates and updates comments correctly") {
    val uuid = UUID.randomUUID()
    val now  = NDLADate.now()
    when(clock.now()).thenReturn(now)
    when(uuidUtil.randomUUID()).thenReturn(uuid)

    val updatedComments =
      List(
        UpdatedCommentDTO(id = None, content = "hei", isOpen = Some(true), solved = Some(false)),
        UpdatedCommentDTO(id = Some(uuid.toString), content = "yoo", isOpen = Some(false), solved = Some(false))
      )
    val existingComments =
      Seq(Comment(id = uuid, created = now, updated = now, content = "nja", isOpen = true, solved = false))
    val expectedComments = Seq(
      Comment(id = uuid, created = now, updated = now, content = "hei", isOpen = true, solved = false),
      Comment(id = uuid, created = now, updated = now, content = "yoo", isOpen = false, solved = false)
    )
    CommonConverter.mergeUpdatedCommentsWithExisting(updatedComments, existingComments) should be(expectedComments)
  }

  test("that mergeUpdatedCommentsWithExisting only keeps updatedComments and deletes rest") {
    val uuid  = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()
    val now   = NDLADate.now()
    when(clock.now()).thenReturn(now)

    val updatedComments = List(
      UpdatedCommentDTO(id = Some(uuid.toString), content = "updated keep", isOpen = Some(true), solved = Some(false))
    )
    val existingComments = Seq(
      Comment(id = uuid, created = now, updated = now, content = "keep", isOpen = true, solved = false),
      Comment(id = uuid2, created = now, updated = now, content = "delete", isOpen = true, solved = false),
      Comment(id = uuid3, created = now, updated = now, content = "delete", isOpen = true, solved = false)
    )
    val expectedComments =
      Seq(Comment(id = uuid, created = now, updated = now, content = "updated keep", isOpen = true, solved = false))
    val result = CommonConverter.mergeUpdatedCommentsWithExisting(updatedComments, existingComments)
    result should be(expectedComments)
  }

  test("that newCommentApiToDomain creates comments correctly") {
    val uuid = UUID.randomUUID()
    val now  = NDLADate.now()
    when(clock.now()).thenReturn(now)

    val newComments     = NewCommentDTO(content = "hei", isOpen = None)
    val expectedComment =
      Comment(id = uuid, created = now, updated = now, content = "hei", isOpen = true, solved = false)
    CommonConverter.newCommentApiToDomain(newComments).copy(id = uuid) should be(expectedComment)
  }
}

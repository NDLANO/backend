/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.draft

import no.ndla.common.model.domain.draft.DraftStatus._
import no.ndla.scalatestsuite.UnitTestSuite

class DraftStatusTest extends UnitTestSuite {
  test("That sort order is the same as definition order") {
    val expected: Seq[DraftStatus] = Seq(
      IMPORTED,
      PLANNED,
      IN_PROGRESS,
      EXTERNAL_REVIEW,
      INTERNAL_REVIEW,
      QUALITY_ASSURANCE,
      LANGUAGE,
      FOR_APPROVAL,
      END_CONTROL,
      PUBLISH_DELAYED,
      PUBLISHED,
      REPUBLISH,
      UNPUBLISHED,
      ARCHIVED
    )
    DraftStatus.values.sorted should be(expected)
  }

}

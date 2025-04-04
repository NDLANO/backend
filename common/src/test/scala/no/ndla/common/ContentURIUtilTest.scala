/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import no.ndla.common.ContentURIUtil.NotUrnPatternException
import no.ndla.testbase.UnitTestSuiteBase

import scala.util.{Failure, Success}

class ContentURIUtilTest extends UnitTestSuiteBase {

  test("That parsing articleId with and without revision works as expected") {
    ContentURIUtil.parseArticleIdAndRevision("urn:article:15") should be((Success(15), None))
    ContentURIUtil.parseArticleIdAndRevision("urn:article:15#10") should be((Success(15), Some(10)))
    ContentURIUtil.parseArticleIdAndRevision("15") should be((Success(15), None))
    ContentURIUtil.parseArticleIdAndRevision("15#100") should be((Success(15), Some(100)))

    val (failed, Some(100)) = ContentURIUtil.parseArticleIdAndRevision("#100")
    failed.isFailure should be(true)
    val (failed2, None) = ContentURIUtil.parseArticleIdAndRevision("")
    failed2.isFailure should be(true)
  }

  test("That non-matching idString will fail and not throw exception") {
    val result = ContentURIUtil.parseArticleIdAndRevision("one")
    result should be(
      (
        Failure(
          NotUrnPatternException("Pattern \"one\" passed to `parseArticleIdAndRevision` did not match urn pattern.")
        ),
        None
      )
    )
  }

  test("That frontpages are matched, but does not match article :^)") {
    ContentURIUtil.parseFrontpageId("urn:frontpage:15") should be(Success(15))
    ContentURIUtil.parseFrontpageId("urn:article:15") should be(
      Failure(
        NotUrnPatternException("Pattern \"urn:article:15\" passed to `parseFrontpageId` did not match urn pattern.")
      )
    )
  }

}

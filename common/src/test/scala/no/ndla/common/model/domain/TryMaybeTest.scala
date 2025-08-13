/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import no.ndla.common.model.domain.TryMaybe.*
import no.ndla.testbase.UnitTestSuiteBase

import scala.util.{Failure, Success, Try}

class TryMaybeTest extends UnitTestSuiteBase {
  test("That flatMap works as expected") {
    val tryMaybe = TryMaybe(Success(Some(1)))
    val result   = tryMaybe.flatMap(v => TryMaybe(Success(Some(v + 1))))
    result should be(TryMaybe(Success(Some(2))))
  }

  test("That map works as expected") {
    val tryMaybe = TryMaybe(Success(Some(1)))
    val result   = tryMaybe.map(v => v + 1)
    result should be(TryMaybe(Success(Some(2))))
  }

  test("That toTrySome works as expected") {
    val normalTry = Success(1)
    val result    = normalTry.toTrySome
    result should be(TryMaybe(Success(Some(1))))

    val normalFail = Failure(new RuntimeException("Bad"))
    val result2    = normalFail.toTrySome
    result2 should be(TryMaybe(normalFail))
  }

  test("That toTryMaybe works as expected") {
    val normalTry = Success(Some(1))
    val result    = normalTry.toTryMaybe
    result should be(TryMaybe(Success(Some(1))))

    val normalFail = Failure(new RuntimeException("Bad"))
    val result2    = normalFail.toTryMaybe
    result2 should be(TryMaybe(normalFail))
  }

  test("That for-comprehensions works as expected") {
    val tryMaybe = for {
      a <- Success(Some(1)).toTryMaybe
      b <- Success(Some(2)).toTryMaybe
    } yield a + b
    tryMaybe should be(TryMaybe(Success(Some(3))))

    val error: Try[Option[Long]] = Failure(new RuntimeException("Bad"))
    val tryMaybe2                = for {
      a <- error.toTryMaybe
      b <- Success(Some(2)).toTryMaybe
    } yield a + b
    tryMaybe2 should be(TryMaybe(error))
  }
}

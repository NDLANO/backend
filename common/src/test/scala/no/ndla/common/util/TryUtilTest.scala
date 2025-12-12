/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.util

import no.ndla.common.TryUtil.*
import no.ndla.testbase.UnitTestSuiteBase

import scala.util.{Failure, Success, Try}

class TryUtilTest extends UnitTestSuiteBase {

  override def afterEach(): Unit = {
    super.afterEach()
    Thread.interrupted() // Make sure interrupt status is cleared after each test
  }

  test("throwIfInterrupted returns Success when not interrupted") {
    val result = Try.throwIfInterrupted {
      42
    }

    result shouldBe Success(42)
  }

  test("throwIfInterrupted returns Failure for non-interrupt exceptions") {
    val boom = new RuntimeException("boom")

    val result = Try.throwIfInterrupted {
      throw boom
    }

    result shouldBe Failure(boom)
    Thread.currentThread().isInterrupted shouldBe false
  }

  test("throwIfInterrupted rethrows when thread was already interrupted") {
    val thread = Thread.currentThread()
    thread.interrupt()

    try {
      intercept[InterruptedException] {
        Try.throwIfInterrupted {
          "value"
        }
      }
    } finally {
      Thread.interrupted()
    }
  }

  test("throwIfInterrupted rethrows when f throws InterruptedException") {
    val ex = intercept[InterruptedException] {
      Try.throwIfInterrupted {
        throw new InterruptedException("boom")
      }
    }

    ex shouldBe a[InterruptedException]
  }

  test("throwIfInterrupted rethrows when InterruptedException is nested in a cause") {
    val wrapper = new RuntimeException("wrap", new InterruptedException("inner"))

    val ex = intercept[InterruptedException] {
      Try.throwIfInterrupted {
        throw wrapper
      }
    }

    ex.getSuppressed.toSeq should contain(wrapper)
  }

  test("throwIfInterrupted rethrows when f interrupts the thread even if it succeeds") {
    try {
      intercept[InterruptedException] {
        Try.throwIfInterrupted {
          Thread.currentThread().interrupt()
          "ok"
        }
      }
    } finally {
      Thread.interrupted() // clear interrupt status for other tests
    }
  }

}

/*
 * Part of NDLA common
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.caching

import no.ndla.testbase.UnitTestSuiteBase
import org.mockito.Mockito.{times, verify, when}

import scala.util.{Success, Try}

class MemoizeTest extends UnitTestSuiteBase {

  class Target {
    def targetMethod(): Try[String] = Success("Hei")
  }

  test("That an uncached value will do an actual call") {
    val targetMock     = mock[Target]
    val memoizedTarget = new Memoize[String](Long.MaxValue, () => targetMock.targetMethod())

    when(targetMock.targetMethod()).thenReturn(Success("Hello from mock"))
    memoizedTarget() should equal(Success("Hello from mock"))
    verify(targetMock, times(1)).targetMethod()
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock     = mock[Target]
    val memoizedTarget = new Memoize[String](Long.MaxValue, () => targetMock.targetMethod())

    when(targetMock.targetMethod()).thenReturn(Success("Hello from mock"))
    Seq(1 to 10).foreach(_ => {
      memoizedTarget() should equal(Success("Hello from mock"))
    })
    verify(targetMock, times(1)).targetMethod()
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 20L
    val targetMock      = mock[Target]
    val memoizedTarget  = new Memoize[String](cacheMaxAgeInMs, () => targetMock.targetMethod())

    when(targetMock.targetMethod()).thenReturn(Success("Hello from mock"))

    memoizedTarget() should equal(Success("Hello from mock"))
    memoizedTarget() should equal(Success("Hello from mock"))
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal(Success("Hello from mock"))
    memoizedTarget() should equal(Success("Hello from mock"))

    verify(targetMock, times(2)).targetMethod()
  }

  test("That an error on first call throws even when retryOnErrorMs is set") {
    val targetMock     = mock[Target]
    val memoizedTarget = new Memoize[String](Long.MaxValue, () => targetMock.targetMethod(), retryOnErrorMs = Some(20L))

    when(targetMock.targetMethod()).thenThrow(new RuntimeException("Boom"))
    intercept[RuntimeException] {
      memoizedTarget()
    }
  }

  test("That the cache is preserved on error when retryOnErrorMs is set") {
    val cacheMaxAgeInMs = 20L
    val retryMs         = 20L
    val targetMock      = mock[Target]
    val memoizedTarget  =
      new Memoize[String](cacheMaxAgeInMs, () => targetMock.targetMethod(), retryOnErrorMs = Some(retryMs))

    when(targetMock.targetMethod()).thenReturn(Success("Hello from mock")).thenThrow(new RuntimeException("Woop"))

    memoizedTarget() should equal(Success("Hello from mock"))
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal(Success("Hello from mock"))
  }
}

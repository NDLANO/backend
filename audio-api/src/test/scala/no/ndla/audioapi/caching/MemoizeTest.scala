/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.caching

import no.ndla.testbase.UnitTestSuiteBase
import org.mockito.Mockito.{times, verify, when}

class MemoizeTest extends UnitTestSuiteBase {

  class Target {
    def targetMethod(): String = "Hei"
  }

  test("That an uncached value will do an actual call") {
    val targetMock     = mock[Target]
    val memoizedTarget = Memoize[String](Long.MaxValue, () => targetMock.targetMethod())

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    verify(targetMock, times(1)).targetMethod()
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock     = mock[Target]
    val memoizedTarget = Memoize[String](Long.MaxValue, () => targetMock.targetMethod())

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    Seq(1 to 10).foreach(_ => {
      memoizedTarget() should equal("Hello from mock")
    })
    verify(targetMock, times(1)).targetMethod()
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 20L
    val targetMock      = mock[Target]
    val memoizedTarget  = Memoize[String](cacheMaxAgeInMs, () => targetMock.targetMethod())

    when(targetMock.targetMethod()).thenReturn("Hello from mock")

    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")

    verify(targetMock, times(2)).targetMethod()
  }
}

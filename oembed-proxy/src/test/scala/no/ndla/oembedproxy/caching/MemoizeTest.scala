/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.caching

import no.ndla.oembedproxy.UnitSuite
import no.ndla.oembedproxy.model.DoNotUpdateMemoizeException
import org.mockito.Mockito._

class MemoizeTest extends UnitSuite {

  class Target {
    def targetMethod(): String = "Hei"
  }

  test("That an uncached value will do an actual call") {
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](Long.MaxValue, Long.MaxValue, targetMock.targetMethod, false)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    verify(targetMock, times(1)).targetMethod()
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](Long.MaxValue, Long.MaxValue, targetMock.targetMethod, false)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    Seq(1 to 10).foreach(i => {
      memoizedTarget() should equal("Hello from mock")
    })
    verify(targetMock, times(1)).targetMethod()
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 20
    val cacheRetryInMs = 20
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](cacheMaxAgeInMs, cacheRetryInMs, targetMock.targetMethod, false)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")

    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")

    verify(targetMock, times(2)).targetMethod()
  }

  test("That the cache is stored on failure") {
    val cacheMaxAgeInMs = 20
    val cacheRetryInMs = 20
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](cacheMaxAgeInMs, cacheRetryInMs, targetMock.targetMethod, false)

    when(targetMock.targetMethod())
      .thenReturn("Hello from mock")
      .andThenThrow(new DoNotUpdateMemoizeException("Woop"))

    memoizedTarget() should equal("Hello from mock")
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal("Hello from mock")
  }
}

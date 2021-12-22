/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.caching

import no.ndla.searchapi.UnitSuite
import org.mockito.Mockito._

class MemoizeTest extends UnitSuite {

  class Target {
    def targetMethod(): String = "Hei"

    def slowTargetMethod(): String = {
      Thread.sleep(500)
      "Slow Hei"
    }
  }

  test("That an uncached value will do an actual call") {
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](10000, targetMock.targetMethod)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    verify(targetMock, times(1)).targetMethod()
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](10000, targetMock.targetMethod)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    Seq(1 to 10).foreach(i => {
      memoizedTarget() should equal("Hello from mock")
    })
    verify(targetMock, times(1)).targetMethod()
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 500
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](cacheMaxAgeInMs, targetMock.targetMethod)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")

    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")

    verify(targetMock, times(2)).targetMethod()
  }

  test("That calling slow function twice will wait for first to finish and only call target once") {
    val targetMock = mock[Target]
    val memoizedTarged = new Memoize[String](10000, targetMock.slowTargetMethod)

    when(targetMock.slowTargetMethod()).thenCallRealMethod()

    memoizedTarged() should be("Slow Hei")
    memoizedTarged() should be("Slow Hei")

    verify(targetMock, times(1)).slowTargetMethod()
  }

}

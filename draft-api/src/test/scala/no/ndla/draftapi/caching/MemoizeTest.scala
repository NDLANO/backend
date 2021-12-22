/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.caching

import no.ndla.draftapi.UnitSuite
import org.mockito.Mockito._

import scala.util.{Failure, Try}

class MemoizeTest extends UnitSuite {

  class Target {
    def targetMethod(): String = "Hei"
  }
  def shouldCacheResult(r: String) = true
  def shouldNotCacheResult(r: String) = false

  test("That an uncached value will do an actual call") {
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](Long.MaxValue, targetMock.targetMethod _, false, shouldCacheResult)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    memoizedTarget() should equal(Some("Hello from mock"))
    verify(targetMock, times(1)).targetMethod()
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](Long.MaxValue, targetMock.targetMethod _, false, shouldCacheResult)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    Seq(1 to 10).foreach(i => {
      memoizedTarget() should equal(Some("Hello from mock"))
    })
    verify(targetMock, times(1)).targetMethod()
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 20
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](cacheMaxAgeInMs, targetMock.targetMethod _, false, shouldCacheResult)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")

    memoizedTarget() should equal(Some("Hello from mock"))
    memoizedTarget() should equal(Some("Hello from mock"))
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal(Some("Hello from mock"))
    memoizedTarget() should equal(Some("Hello from mock"))

    verify(targetMock, times(2)).targetMethod()
  }

  test("The cache should only be renewed if shouldCacheResult returns true") {
    val cacheMaxAgeInMs = 20
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[String](cacheMaxAgeInMs, targetMock.targetMethod _, false, shouldNotCacheResult)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")

    memoizedTarget() should equal(None)
    memoizedTarget() should equal(None)

    verify(targetMock, times(2)).targetMethod()
  }

}

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.caching

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import no.ndla.draftapi.DraftApiProperties.ApiClientsCacheAgeInMs

private[caching] class Memoize[R](maxCacheAgeMs: Long,
                                  f: () => R,
                                  autoRefreshCache: Boolean,
                                  shouldCacheResult: R => Boolean)
    extends (() => Option[R]) {
  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  private[this] var cache: Option[CacheValue] = None

  private def renewCache = {
    val result = f()

    if (shouldCacheResult(result))
      cache = Some(CacheValue(result, System.currentTimeMillis()))
  }

  if (autoRefreshCache) {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() = renewCache
    }
    ex.scheduleAtFixedRate(task, 20, maxCacheAgeMs, TimeUnit.MILLISECONDS)
  }

  def apply(): Option[R] = {
    cache match {
      case Some(cachedValue) if autoRefreshCache       => Some(cachedValue.value)
      case Some(cachedValue) if !cachedValue.isExpired => Some(cachedValue.value)
      case _ =>
        renewCache
        cache.map(_.value)
    }
  }

}

object Memoize {

  def apply[R](f: () => R, shouldCacheResult: R => Boolean = (_: R) => true) =
    new Memoize(ApiClientsCacheAgeInMs, f, autoRefreshCache = false, shouldCacheResult)
}

object MemoizeAutoRenew {

  def apply[R](f: () => R, shouldCacheResult: R => Boolean = (_: R) => true) =
    new Memoize(ApiClientsCacheAgeInMs, f, autoRefreshCache = true, shouldCacheResult)
}

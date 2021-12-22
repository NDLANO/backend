/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.caching

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import no.ndla.oembedproxy.OEmbedProxyProperties.{ProviderListCacheAgeInMs, ProviderListRetryTimeInMs}
import no.ndla.oembedproxy.model.DoNotUpdateMemoizeException

class Memoize[R](maxCacheAgeMs: Long, retryTimeInMs: Long, f: () => R, autoRefreshCache: Boolean)
    extends (() => R)
    with LazyLogging {

  case class CacheValue(value: R, lastUpdated: Long) {

    def isExpired: Boolean =
      lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  private[this] var cache: Option[CacheValue] = None

  private def renewCache = {
    try {
      cache = Some(CacheValue(f(), System.currentTimeMillis()))
    } catch {
      case mex: DoNotUpdateMemoizeException =>
        val retryTime = System
          .currentTimeMillis() - maxCacheAgeMs + retryTimeInMs
        cache = Some(CacheValue(cache.get.value, retryTime))
        logger.warn(
          s"Caught ${mex.getClass.getName}, with message: '${mex.getMessage}', will not update cached output.")
    }
  }

  if (autoRefreshCache) {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() = renewCache
    }
    ex.scheduleAtFixedRate(task, 20, maxCacheAgeMs, TimeUnit.MILLISECONDS)
  }

  def apply(): R = {
    cache match {
      case Some(cachedValue) if autoRefreshCache       => cachedValue.value
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ =>
        renewCache
        cache.get.value
    }
  }
}

object Memoize {

  def apply[R](f: () => R) =
    new Memoize(ProviderListCacheAgeInMs, ProviderListRetryTimeInMs, f, autoRefreshCache = false)
}

object MemoizeAutoRenew {

  def apply[R](f: () => R) =
    new Memoize(ProviderListCacheAgeInMs, ProviderListRetryTimeInMs, f, autoRefreshCache = true)
}

/*
 * Part of NDLA common
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.caching

import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

class Memoize[R](
    maxCacheAgeMs: Long,
    f: () => R,
    autoRefreshCache: Boolean = false,
    retryOnErrorMs: Option[Long] = None,
) extends (() => R)
    with StrictLogging {

  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  @volatile
  private var cache: Option[CacheValue] = None

  private def renewCache(): Unit = {
    try {
      cache = Some(CacheValue(f(), System.currentTimeMillis()))
    } catch {
      case ex: Throwable => (retryOnErrorMs, cache) match {
          case (Some(retryMs), Some(cacheValue)) =>
            val retryTime = System.currentTimeMillis() - maxCacheAgeMs + retryMs
            cache = Some(cacheValue.copy(lastUpdated = retryTime))
            logger.warn(
              s"Caught ${ex.getClass.getName}, with message: '${ex.getMessage}', will not update cached output."
            )
          case _ => throw ex
        }
    }
  }

  if (autoRefreshCache) {
    val ex   = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run(): Unit = renewCache()
    }
    ex.scheduleAtFixedRate(task, 20, maxCacheAgeMs, TimeUnit.MILLISECONDS): Unit
  }

  def apply(): R = {
    cache match {
      case Some(cachedValue) if autoRefreshCache       => cachedValue.value
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _                                           =>
        renewCache()
        cache.get.value
    }
  }
}

object Memoize {
  def apply[R](maxCacheAgeMs: Long, f: () => R): Memoize[R] = new Memoize(maxCacheAgeMs, f)
}

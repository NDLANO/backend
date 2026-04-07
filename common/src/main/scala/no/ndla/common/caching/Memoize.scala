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
import java.util.concurrent.atomic.AtomicReference
import scala.util.{Failure, Try, Success}

class Memoize[R](
    maxCacheAgeMs: Long,
    f: () => Try[R],
    autoRefreshCache: Boolean = false,
    retryOnErrorMs: Option[Long] = None,
) extends (() => Try[R])
    with StrictLogging {

  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

  private val cache: AtomicReference[Option[CacheValue]] = new AtomicReference(None)

  private def setCache(value: R): Try[R]                  = setCache(value, System.currentTimeMillis())
  private def setCache(value: R, cacheTime: Long): Try[R] = {
    cache.set(Some(CacheValue(value, cacheTime)))
    Success(value)
  }

  def setCacheTime(cacheTime: Long): Unit = {
    cache.updateAndGet {
      case Some(cacheValue) => Some(cacheValue.copy(lastUpdated = cacheTime))
      case None =>
        logger.warn(s"Attempted to set cache time to $cacheTime, but no cached value exists.")
        None
    }: Unit
  }

  private def recoverFailure(ex: Throwable): Try[R] = {
    (retryOnErrorMs, cache.get()) match {
      case (Some(retryMs), Some(cacheValue)) =>
        val retryTime = System.currentTimeMillis() - maxCacheAgeMs + retryMs
        setCacheTime(retryTime)
        logger.warn(s"Caught ${ex.getClass.getName}, with message: '${ex.getMessage}', will not update cached output.")
        Success(cacheValue.value)
      case _ =>
        logger.warn(
          s"Caught ${ex.getClass.getName}, with message: '${ex.getMessage}', no cached output to fall back to."
        )
        Failure(ex)
    }
  }

  private def renewCache(): Try[R] = {
    try {
      val callResult = f()
      callResult match {
        case Success(result) => setCache(result)
        case Failure(ex)     => recoverFailure(ex)
      }
    } catch {
      case ex: Throwable => recoverFailure(ex) match {
          case Failure(exception) => throw exception
          case Success(value)     => Success(value)
        }
    }
  }

  if (autoRefreshCache) {
    val threadPool = new ScheduledThreadPoolExecutor(1)
    val task       = new Runnable {
      def run(): Unit = renewCache(): Unit
    }
    threadPool.scheduleAtFixedRate(task, 20, maxCacheAgeMs, TimeUnit.MILLISECONDS): Unit
  }

  def apply(): Try[R] = {
    cache.get() match {
      case Some(cachedValue) if autoRefreshCache       => Success(cachedValue.value)
      case Some(cachedValue) if !cachedValue.isExpired => Success(cachedValue.value)
      case _                                           => renewCache()
    }
  }
}

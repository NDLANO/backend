/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.caching

import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success}

class Memoize[R](maxCacheAgeMs: Long, f: () => R) extends (() => R) with LazyLogging {
  implicit val ec: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))

  case class CacheValue(value: R, lastUpdated: Long) {

    def isExpired: Boolean =
      lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }
  private[this] var cache: Option[CacheValue] = None
  private[this] var isUpdating: Option[Future[CacheValue]] = None

  private def scheduleRenewCache(): Future[CacheValue] = synchronized {
    val fut = Future {
      CacheValue(f(), System.currentTimeMillis())
    }

    isUpdating = Some(fut)

    fut.onComplete {
      case Success(value) => updateCache(value)
      case Failure(ex)    => logger.error(s"Failed to update memoized function. Failed with: ${ex.getMessage}", ex)
    }
    fut
  }

  def apply(): R = {
    cache match {
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ =>
        val fut = isUpdating match {
          case Some(future) => future
          case None         => scheduleRenewCache()
        }

        Await.result(fut, Duration.Inf).value
    }
  }

  def updateCache(result: CacheValue): Unit = {
    isUpdating = None
    cache = Some(result)
    System.gc()
  }
}

object Memoize {

  def apply[R](f: () => R) =
    new Memoize(1000 * 60, f)
}

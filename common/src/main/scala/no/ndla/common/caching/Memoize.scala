/*
 * Part of NDLA common
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.caching

import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.Executors
import scala.collection.mutable.Map as MutableMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success}

class Memoize[I, R](maxCacheAgeMs: Long, f: I => R) extends StrictLogging {
  implicit val ec: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))

  case class CacheValue(input: I, value: R, lastUpdated: Long) {

    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }
  private val cache: MutableMap[I, CacheValue]              = MutableMap.empty
  private val isUpdating: MutableMap[I, Future[CacheValue]] = MutableMap.empty

  private def scheduleRenewCache(input: I): Future[CacheValue] = synchronized {
    val fut = Future {
      CacheValue(input, f(input), System.currentTimeMillis())
    }

    isUpdating.put(input, fut): Unit

    fut.onComplete {
      case Success(value) => updateCache(value)
      case Failure(ex)    => logger.error(s"Failed to update memoized function. Failed with: ${ex.getMessage}", ex)
    }
    fut
  }

  def apply(input: I): R = {
    cache.get(input) match {
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _                                           =>
        val fut = isUpdating.get(input) match {
          case Some(value) => value
          case None        => scheduleRenewCache(input)
        }

        Await.result(fut, 20.minutes).value
    }
  }

  private def updateCache(result: CacheValue): Unit = {
    isUpdating.remove(result.input): Unit
    cache.put(result.input, result): Unit
    System.gc()
  }
}

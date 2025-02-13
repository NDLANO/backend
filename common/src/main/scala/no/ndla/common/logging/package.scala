/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.*

package object logging extends StrictLogging {
  def logTaskTime[T](taskName: String, warnLimit: Duration = Duration.Zero)(task: => T): T = {
    val start  = System.nanoTime()
    val result = task
    val end    = System.nanoTime()

    val taskDuration = Duration.fromNanos(end - start).toMillis.millis

    if (warnLimit != Duration.Zero && taskDuration >= warnLimit) {
      logger.warn(s"Task $taskName took $taskDuration")
    } else {
      logger.info(s"Task $taskName took $taskDuration")
    }
    result
  }
}

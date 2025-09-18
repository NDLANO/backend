/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.errors

import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Try}

object ExceptionLogHandler extends StrictLogging {
  private def handleException(e: Throwable): Unit = {
    logger.error(s"Uncaught exception, quitting...", e)
    System.exit(1)
  }

  def default(f: => Try[Unit]): Unit = {
    try {
      f match {
        case Failure(ex) => handleException(ex)
        case _           => System.exit(0)
      }
    } catch {
      case ex: Throwable => handleException(ex)
    }
  }
}

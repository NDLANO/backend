/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.errors

import org.log4s.Logger

import scala.util.{Failure, Try}

object ExceptionLogHandler {
  val logger: Logger = org.log4s.getLogger

  private def handleException(e: Throwable): Unit = {
    logger.error(e)(s"Uncaught exception, quitting...")
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

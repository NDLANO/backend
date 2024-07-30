/*
 * Part of NDLA backend.common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.errors

import org.log4s.Logger

object ExceptionLogHandler {
  val logger: Logger = org.log4s.getLogger
  def default(f: => Unit): Unit = {
    try {
      f
    } catch {
      case e: Throwable =>
        logger.error(e)(s"Uncaught exception, quitting...")
        System.exit(1)
    }
  }

}

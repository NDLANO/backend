/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.logging

import cats.effect.IO
import com.typesafe.scalalogging.Logger
import no.ndla.common.logging.{FLogger, LoggerContext, LoggerInfo}
import no.ndla.network.model.RequestInfo
import org.slf4j.LoggerFactory

trait FLogging {
  private val delegate = Logger(LoggerFactory.getLogger(getClass.getName))
  def logger: FLogger  = new FLogger(delegate)

  /** Implicit context used to derive required [[LoggerInfo]] */
  implicit val ioLoggerContext: LoggerContext[IO] = new LoggerContext[IO] {
    override def get: IO[LoggerInfo] = RequestInfo.get.map(info => LoggerInfo(correlationId = info.correlationId))
    override def map[T](f: LoggerInfo => T): IO[T] = get.map(f)
  }
}

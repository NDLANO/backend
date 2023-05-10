/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.logging

import com.typesafe.scalalogging.Logger
import org.slf4j.{LoggerFactory, MDC}

trait Flogger {
  private val delegate = Logger(LoggerFactory.getLogger(getClass.getName))
  protected def logger: FLogger = new FLogger(delegate)
}

class FLogger(delegate: Logger) {
  private def withMDC[F[_]: LoggerContext, T](t: => T): F[T] =
    implicitly[LoggerContext[F]].map { info =>
      MDC.put("correlationID", info.correlationId)
      try t
      finally MDC.clear()
    }

  def debug[F[_]: LoggerContext](message: String): F[Unit] = withMDC(delegate.debug(message))
  def debug[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(delegate.debug(message, cause))
  def info[F[_]: LoggerContext](message: String): F[Unit] = withMDC(delegate.info(message))
  def info[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(delegate.info(message, cause))
  def warn[F[_]: LoggerContext](message: String): F[Unit] = withMDC(delegate.warn(message))
  def warn[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(delegate.warn(message, cause))
  def error[F[_]: LoggerContext](message: String): F[Unit] = withMDC(delegate.error(message))
  def error[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(delegate.error(message, cause))
}

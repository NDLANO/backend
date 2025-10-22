/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.logging

import com.typesafe.scalalogging.Logger
import org.slf4j.MDC

case class FLogger(underlying: Logger) {
  private def withMDC[F[_]: LoggerContext, T](t: => T): F[T] = implicitly[LoggerContext[F]].map { info =>
    info.correlationId.foreach(cid => MDC.put(FLogger.correlationIdKey, cid))
    try t
    finally MDC.remove(FLogger.correlationIdKey)
  }

  def debug[F[_]: LoggerContext](message: String): F[Unit]                   = withMDC(underlying.debug(message))
  def debug[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(underlying.debug(message, cause))
  def debug[F[_]: LoggerContext](cause: Throwable)(message: String): F[Unit] = withMDC(underlying.debug(message, cause))
  def debugNoContext(message: String): Unit                                  = underlying.debug(message)
  def debugNoContext(message: String, cause: Throwable): Unit                = underlying.debug(message, cause)
  def debugNoContext(cause: Throwable)(message: String): Unit                = underlying.debug(message, cause)

  def info[F[_]: LoggerContext](message: String): F[Unit]                   = withMDC(underlying.info(message))
  def info[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(underlying.info(message, cause))
  def info[F[_]: LoggerContext](cause: Throwable)(message: String): F[Unit] = withMDC(underlying.info(message, cause))
  def infoNoContext(message: String): Unit                                  = underlying.info(message)
  def infoNoContext(message: String, cause: Throwable): Unit                = underlying.info(message, cause)
  def infoNoContext(cause: Throwable)(message: String): Unit                = underlying.info(message, cause)

  def warn[F[_]: LoggerContext](message: String): F[Unit]                   = withMDC(underlying.warn(message))
  def warn[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(underlying.warn(message, cause))
  def warn[F[_]: LoggerContext](cause: Throwable)(message: String): F[Unit] = withMDC(underlying.warn(message, cause))
  def warnNoContext(message: String): Unit                                  = underlying.warn(message)
  def warnNoContext(message: String, cause: Throwable): Unit                = underlying.warn(message, cause)
  def warnNoContext(cause: Throwable)(message: String): Unit                = underlying.warn(message, cause)

  def error[F[_]: LoggerContext](message: String): F[Unit]                   = withMDC(underlying.error(message))
  def error[F[_]: LoggerContext](message: String, cause: Throwable): F[Unit] = withMDC(underlying.error(message, cause))
  def error[F[_]: LoggerContext](cause: Throwable)(message: String): F[Unit] = withMDC(underlying.error(message, cause))
  def errorNoContext(message: String): Unit                                  = underlying.error(message)
  def errorNoContext(message: String, cause: Throwable): Unit                = underlying.error(message, cause)
  def errorNoContext(cause: Throwable)(message: String): Unit                = underlying.error(message, cause)
}

object FLogger {
  private val correlationIdKey = "correlationID"
}

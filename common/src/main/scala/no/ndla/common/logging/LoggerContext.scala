/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.logging

// Covariance here improves type inference.
// See: https://groups.google.com/g/scala-language/c/dQEomVCH3CI
trait LoggerContext[+F[_]] {
  def get: F[LoggerInfo]
  def map[T](f: LoggerInfo => T): F[T]
}

case class LoggerInfo(correlationId: Option[String])

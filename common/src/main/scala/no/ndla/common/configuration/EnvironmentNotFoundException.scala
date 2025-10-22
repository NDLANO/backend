/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

case class EnvironmentNotFoundException(message: String) extends RuntimeException(message)

object EnvironmentNotFoundException {
  def singleKey(key: String): EnvironmentNotFoundException =
    new EnvironmentNotFoundException(s"Unable to load property $key")
}

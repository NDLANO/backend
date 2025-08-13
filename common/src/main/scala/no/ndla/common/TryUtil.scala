/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import scala.util.{Failure, Success, Try}

object TryUtil {
  def failureIf(condition: Boolean, ex: Throwable): Try[Unit] = if (condition) Failure(ex) else Success(())
}

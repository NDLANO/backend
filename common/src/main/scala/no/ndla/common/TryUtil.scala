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
  extension (tryObj: Try.type) {

    /** If the condition is satisfied, return the given `A` in `Success`, otherwise, return the given `Throwable` in
      * `Failure`.
      */
    def cond[A](cond: Boolean)(value: => A, ex: => Throwable): Try[A] = {
      if (cond) Success(value)
      else Failure(ex)
    }
  }

  extension (failure: Failure.type) {

    /** If the condition is satisfied, return the given `Throwable` in `Failure`, otherwise, return a `Unit` in
      * `Success`.
      */
    def when[A](cond: Boolean)(ex: => Throwable): Try[Unit] = {
      if (cond) Failure(ex)
      else Success(())
    }
  }
}

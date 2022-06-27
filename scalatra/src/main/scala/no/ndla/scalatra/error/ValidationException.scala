/*
 * Part of NDLA scalatra.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatra.error

case class ValidationException(
    message: String = "Validation Error",
    errors: Seq[ValidationMessage]
) extends RuntimeException(message)

object ValidationException {
  def apply(path: String, msg: String) = new ValidationException(errors = Seq(ValidationMessage(path, msg)))
}

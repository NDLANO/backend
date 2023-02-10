/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.errors

case class ValidationException(
    message: String = "Validation Error",
    errors: Seq[ValidationMessage]
) extends RuntimeException(s"$message: Errors: [${errors.map(_.toString).mkString(",")}]")

object ValidationException {
  def apply(path: String, msg: String) = new ValidationException(errors = Seq(ValidationMessage(path, msg)))
}

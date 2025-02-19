/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.errors

import no.ndla.common.errors.ValidationException.formatError

case class ValidationException(
    message: String = "Validation Error",
    errors: Seq[ValidationMessage]
) extends RuntimeException(formatError(message, errors))

object ValidationException {
  def apply(path: String, msg: String) = new ValidationException(errors = Seq(ValidationMessage(path, msg)))

  def formatError(message: String, errors: Seq[ValidationMessage]): String = {
    if (errors.nonEmpty) s"$message:\n${errors.map(e => s"\t${e.field}: ${e.message}").mkString("\n")}"
    else message
  }
}

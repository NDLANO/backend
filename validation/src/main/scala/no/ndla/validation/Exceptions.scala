/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

class ConfigurationException(message: String) extends RuntimeException(message)

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)

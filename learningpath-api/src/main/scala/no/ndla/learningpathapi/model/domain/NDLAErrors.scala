/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.model.api.ValidationMessage

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)
case class AccessDeniedException(message: String)         extends RuntimeException(message)
class OptimisticLockException(message: String)            extends RuntimeException(message)
class ImportException(message: String)                    extends RuntimeException(message)
case class ElasticIndexingException(message: String)      extends RuntimeException(message)
class ResultWindowTooLargeException(message: String)      extends RuntimeException(message)
case class LanguageNotSupportedException(message: String) extends RuntimeException(message)
case class InvalidStatusException(message: String)        extends RuntimeException(message)
case class SearchException(message: String)               extends RuntimeException(message)
case class NotFoundException(message: String)             extends RuntimeException(message)
case class TaxonomyUpdateException(message: String)       extends RuntimeException(message)
case class InvalidOembedResponse(message: String)         extends RuntimeException(message)

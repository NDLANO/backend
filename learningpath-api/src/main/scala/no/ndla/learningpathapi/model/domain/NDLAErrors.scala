/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

class OptimisticLockException(message: String)       extends RuntimeException(message)
class ImportException(message: String)               extends RuntimeException(message)
case class ElasticIndexingException(message: String) extends RuntimeException(message)
class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
case class SearchException(message: String)          extends RuntimeException(message)
case class TaxonomyUpdateException(message: String)  extends RuntimeException(message)
case class InvalidOembedResponse(message: String)    extends RuntimeException(message)

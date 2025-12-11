/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model

case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ImportException(message: String)                             extends RuntimeException(message)
class ImportExceptions(val message: String, val errors: Seq[Throwable]) extends RuntimeException(message)
class ConfigurationException(message: String)                           extends RuntimeException(message)

/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import no.ndla.audioapi.Props
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, FileTooBigException, NotFoundException, ValidationException}
import no.ndla.database.DataSource
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, ErrorBody, TapirErrorHandling, ValidationErrorBody}
import no.ndla.search.NdlaSearchException
import org.postgresql.util.PSQLException

case class CouldNotFindLanguageException(message: String) extends RuntimeException(message)
class AudioStorageException(message: String)              extends RuntimeException(message)
class LanguageMappingException(message: String)           extends RuntimeException(message)
class ImportException(message: String)                    extends RuntimeException(message)
case class JobAlreadyFoundException(message: String)      extends RuntimeException(message)

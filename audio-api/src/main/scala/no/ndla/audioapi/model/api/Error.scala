/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api


case class CouldNotFindLanguageException(message: String) extends RuntimeException(message)
class AudioStorageException(message: String)              extends RuntimeException(message)
class LanguageMappingException(message: String)           extends RuntimeException(message)
class ImportException(message: String)                    extends RuntimeException(message)
case class JobAlreadyFoundException(message: String)      extends RuntimeException(message)
class OptimisticLockException(message: String)            extends RuntimeException(message)

/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model

class ImageNotFoundException(message: String) extends RuntimeException(message)

class ImportException(message: String) extends RuntimeException(message)

case class InvalidUrlException(message: String) extends RuntimeException(message)

class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
case class ElasticIndexingException(message: String) extends RuntimeException(message)

class ImageStorageException(message: String)         extends RuntimeException(message)
case class ImageConversionException(message: String) extends RuntimeException(message)
case class MissingIdException(message: String)       extends RuntimeException(message)
case class ImageCopyException(message: String)       extends RuntimeException(message)

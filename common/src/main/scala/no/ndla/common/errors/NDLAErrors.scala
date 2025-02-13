/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common.errors

case class AccessDeniedException(message: String, unauthorized: Boolean = false) extends RuntimeException(message)

object AccessDeniedException {
  def unauthorized: AccessDeniedException =
    AccessDeniedException("User is missing required permission(s) to perform this operation", unauthorized = true)
  def forbidden: AccessDeniedException =
    AccessDeniedException("User is missing required permission(s) to perform this operation")
}
case class NotFoundException(message: String)             extends RuntimeException(message)
case class RollbackException(ex: Throwable)               extends RuntimeException {
  this.initCause(ex)
}
case class FileTooBigException()                          extends RuntimeException
case class InvalidStatusException(message: String)        extends RuntimeException(message)
case class InvalidStateException(message: String)         extends RuntimeException(message)
case class TokenRetrievalException(message: String)       extends RuntimeException(message)
case class TokenDecodingException(message: String)        extends RuntimeException(message)
case class VideoSourceRetrievalException(message: String) extends RuntimeException(message)
case class VideoSourceParsingException(message: String)   extends RuntimeException(message)

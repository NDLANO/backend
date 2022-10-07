package no.ndla.common.errors

case class AccessDeniedException(message: String, unauthorized: Boolean = false) extends RuntimeException(message)
case class RollbackException(ex: Throwable)                                      extends RuntimeException

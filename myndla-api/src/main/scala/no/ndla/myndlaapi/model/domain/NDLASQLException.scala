/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

case class NDLASQLException(message: String)       extends RuntimeException(message)
case class InvalidStatusException(message: String) extends RuntimeException(message)
case class FolderSortException(message: String)    extends RuntimeException(message)

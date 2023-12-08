/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

case class NDLASQLException(message: String)       extends RuntimeException(message)
case class InvalidStatusException(message: String) extends RuntimeException(message)
case class FolderSortException(message: String)    extends RuntimeException(message)

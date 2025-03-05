/*
 * Part of NDLA search
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search.model.domain

case class DocumentConflictException(message: String) extends RuntimeException(message)

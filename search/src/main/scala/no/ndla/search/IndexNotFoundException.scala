/*
 * Part of NDLA search
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.search

case class IndexNotFoundException(message: String) extends RuntimeException(message)

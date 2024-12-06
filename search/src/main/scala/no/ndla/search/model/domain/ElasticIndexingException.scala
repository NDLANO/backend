/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search.model.domain

case class ElasticIndexingException(message: String) extends RuntimeException(message)

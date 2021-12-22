/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

case class Status(
    current: String,
    other: Seq[String]
)

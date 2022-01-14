/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import no.ndla.language.model.WithLanguage

case class Tag(tags: Seq[String], language: String) extends WithLanguage

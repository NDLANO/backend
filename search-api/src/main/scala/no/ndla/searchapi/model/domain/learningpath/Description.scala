/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.language.model.WithLanguage

case class Description(description: String, language: String) extends WithLanguage

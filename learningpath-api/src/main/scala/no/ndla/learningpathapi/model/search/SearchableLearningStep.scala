/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

import no.ndla.search.model.SearchableLanguageValues

case class SearchableLearningStep(stepType: String,
                                  embedUrl: List[String],
                                  status: String,
                                  titles: SearchableLanguageValues,
                                  descriptions: SearchableLanguageValues)

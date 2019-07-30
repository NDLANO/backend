/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.search

import no.ndla.conceptapi.model.domain

case class SearchableConcept(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    metaImage: Seq[domain.ConceptMetaImage],
    defaultTitle: Option[String]
)

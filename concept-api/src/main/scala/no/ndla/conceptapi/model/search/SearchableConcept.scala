/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.search

import no.ndla.conceptapi.model.domain
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}
import org.joda.time.DateTime

case class SearchableConcept(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    metaImage: Seq[domain.ConceptMetaImage],
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    subjectIds: Seq[String],
    lastUpdated: DateTime,
    status: Status,
    updatedBy: Seq[String],
    license: Option[String],
    copyright: Option[SearchableCopyright],
    embedResourcesAndIds: List[EmbedValues],
    visualElement: SearchableLanguageValues,
    articleIds: Seq[Long],
    created: DateTime,
    source: Option[String]
)

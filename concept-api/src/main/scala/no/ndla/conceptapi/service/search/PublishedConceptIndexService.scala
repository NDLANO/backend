/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service.search

import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.repository.{PublishedConceptRepository, Repository}

trait PublishedConceptIndexService {
  this: IndexService & PublishedConceptRepository & SearchConverterService & Props =>
  lazy val publishedConceptIndexService: PublishedConceptIndexService

  class PublishedConceptIndexService extends IndexService {
    override val documentType: String            = props.ConceptSearchDocument
    override val searchIndex: String             = props.PublishedConceptSearchIndex
    override val repository: Repository[Concept] = publishedConceptRepository
  }

}

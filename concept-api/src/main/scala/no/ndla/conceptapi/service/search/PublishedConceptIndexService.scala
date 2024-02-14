/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.typesafe.scalalogging.StrictLogging
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.domain.Concept
import no.ndla.conceptapi.repository.{PublishedConceptRepository, Repository}

trait PublishedConceptIndexService {
  this: IndexService with PublishedConceptRepository with SearchConverterService with Props =>
  val publishedConceptIndexService: PublishedConceptIndexService

  class PublishedConceptIndexService extends StrictLogging with IndexService {
    override val documentType: String            = props.ConceptSearchDocument
    override val searchIndex: String             = props.PublishedConceptSearchIndex
    override val repository: Repository[Concept] = publishedConceptRepository
  }

}

/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.repository.{DraftConceptRepository, Repository}

trait DraftConceptIndexService {
  this: IndexService & DraftConceptRepository & SearchConverterService & Props =>
  val draftConceptIndexService: DraftConceptIndexService

  class DraftConceptIndexService extends StrictLogging with IndexService {
    override val documentType: String            = props.ConceptSearchDocument
    override val searchIndex: String             = props.DraftConceptSearchIndex
    override val repository: Repository[Concept] = draftConceptRepository
  }

}

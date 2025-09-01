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
import no.ndla.conceptapi.repository.{DraftConceptRepository, Repository}
import no.ndla.search.{NdlaE4sClient, SearchLanguage}

class DraftConceptIndexService(using
    draftConceptRepository: DraftConceptRepository,
    searchConverterService: SearchConverterService,
    props: Props,
    e4sClient: NdlaE4sClient,
    searchLanguage: SearchLanguage
) extends IndexService {
  override val documentType: String            = props.ConceptSearchDocument
  override val searchIndex: String             = props.DraftConceptSearchIndex
  override val repository: Repository[Concept] = draftConceptRepository
}

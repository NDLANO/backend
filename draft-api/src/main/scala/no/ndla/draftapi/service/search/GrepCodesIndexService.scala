/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.search.SearchableGrepCode
import no.ndla.draftapi.repository.{DraftRepository, Repository}
import no.ndla.search.{NdlaE4sClient, SearchLanguage}

class GrepCodesIndexService(using
    searchConverterService: SearchConverterService,
    draftRepository: DraftRepository,
    props: Props,
    e4sClient: NdlaE4sClient,
    searchLanguage: SearchLanguage
) extends IndexService[Draft, SearchableGrepCode]
    with StrictLogging {
  override val documentType: String          = props.DraftGrepCodesSearchDocument
  override val searchIndex: String           = props.DraftGrepCodesSearchIndex
  override val repository: Repository[Draft] = draftRepository

  override def createIndexRequests(domainModel: Draft, indexName: String): Seq[IndexRequest] = {
    val grepCodes = searchConverterService.asSearchableGrepCodes(domainModel)

    grepCodes.map(code => {
      val source = CirceUtil.toJsonString(code)
      indexInto(indexName).doc(source).id(code.grepCode)
    })
  }

  def getMapping: MappingDefinition = {
    properties(List(textField("grepCode")))
  }
}

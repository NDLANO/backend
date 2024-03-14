/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.search.SearchableTag
import no.ndla.draftapi.repository.{DraftRepository, Repository}

trait TagIndexService {
  this: SearchConverterService with IndexService with DraftRepository with Props =>
  val tagIndexService: TagIndexService

  class TagIndexService extends StrictLogging with IndexService[Draft, SearchableTag] {
    override val documentType: String          = props.DraftTagSearchDocument
    override val searchIndex: String           = props.DraftTagSearchIndex
    override val repository: Repository[Draft] = draftRepository

    override def createIndexRequests(domainModel: Draft, indexName: String): Seq[IndexRequest] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      tags.map(t => {
        val source = CirceUtil.toJsonString(t)
        indexInto(indexName).doc(source).id(s"${t.language}.${t.tag}")
      })
    }

    def getMapping: MappingDefinition = {
      properties(
        List(
          textField("tag"),
          keywordField("language")
        )
      )
    }
  }

}

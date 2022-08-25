/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.search.SearchableTag
import no.ndla.draftapi.repository.{DraftRepository, Repository}
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization.write

trait TagIndexService {
  this: SearchConverterService with IndexService with DraftRepository with Props =>
  val tagIndexService: TagIndexService

  class TagIndexService extends LazyLogging with IndexService[Draft, SearchableTag] {
    implicit val formats: Formats              = SearchableLanguageFormats.JSonFormats
    override val documentType: String          = props.DraftTagSearchDocument
    override val searchIndex: String           = props.DraftTagSearchIndex
    override val repository: Repository[Draft] = draftRepository

    override def createIndexRequests(domainModel: Draft, indexName: String): Seq[IndexRequest] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      tags.map(t => {
        val source = write(t)
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

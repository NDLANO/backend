/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.domain.Article
import no.ndla.draftapi.model.search.{SearchableLanguageFormats, SearchableTag}
import no.ndla.draftapi.repository.{DraftRepository, Repository}
import org.json4s.native.Serialization.write

trait TagIndexService {
  this: SearchConverterService with IndexService with DraftRepository =>
  val tagIndexService: TagIndexService

  class TagIndexService extends LazyLogging with IndexService[Article, SearchableTag] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = DraftApiProperties.DraftTagSearchDocument
    override val searchIndex: String = DraftApiProperties.DraftTagSearchIndex
    override val repository: Repository[Article] = draftRepository

    override def createIndexRequests(domainModel: Article, indexName: String): Seq[IndexRequest] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      tags.map(t => {
        val source = write(t)
        indexInto(indexName / documentType).doc(source).id(s"${t.language}.${t.tag}")
      })
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          textField("tag"),
          keywordField("language")
        )
      )
    }
  }

}

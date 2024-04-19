/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ObjectField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.DraftConceptApiClient
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.search.SearchType
import no.ndla.common.model.domain.concept.Concept

import scala.util.Try

trait DraftConceptIndexService {
  this: SearchConverterService & IndexService & DraftConceptApiClient & Props =>
  val draftConceptIndexService: DraftConceptIndexService

  class DraftConceptIndexService extends StrictLogging with IndexService[Concept] {
    import props.SearchIndex
    override val documentType: String             = "concept"
    override val searchIndex: String              = SearchIndex(SearchType.Concepts)
    override val apiClient: DraftConceptApiClient = draftConceptApiClient

    override def createIndexRequest(
        domainModel: Concept,
        indexName: String,
        indexingBundle: IndexingBundle
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableConcept(domainModel, indexingBundle).map { searchable =>
        val source = CirceUtil.toJsonString(searchable)
        indexInto(indexName).doc(source).id(domainModel.id.get.toString)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        longField("id"),
        keywordField("conceptType"),
        nestedField("metaImage").fields(
          keywordField("imageId"),
          keywordField("altText"),
          keywordField("language")
        ),
        keywordField("defaultTitle"),
        keywordField("subjectIds"),
        dateField("lastUpdated"),
        keywordField("status.current"),
        keywordField("status.other"),
        keywordField("updatedBy"),
        keywordField("license"),
        keywordField("authors"),
        longField("articleIds"),
        dateField("created"),
        keywordField("learningResourceType"),
        keywordField("source"),
        ObjectField(
          "responsible",
          properties = Seq(
            keywordField("responsibleId"),
            dateField("lastUpdated")
          )
        ),
        textField("gloss"),
        longField("favorited"),
        ObjectField("domainObject", enabled = Some(false))
      )
      val dynamics =
        generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
          generateLanguageSupportedDynamicTemplates("content", keepRaw = true) ++
          generateLanguageSupportedDynamicTemplates("tags")

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}

/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ObjectField
import com.sksamuel.elastic4s.requests.common.VersionType.EXTERNAL_GTE
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.api.search.SearchType
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.DraftConceptApiClient
import no.ndla.searchapi.model.domain.IndexingBundle
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
        indexInto(indexName)
          .doc(source)
          .id(domainModel.id.get.toString)
          .versionType(EXTERNAL_GTE)
          .version(domainModel.revision.map(_.toLong).get)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        longField("id"),
        keywordField("conceptType"),
        keywordField("defaultTitle"),
        dateField("lastUpdated"),
        keywordField("draftStatus.current"),
        keywordField("draftStatus.other"),
        keywordField("status"),
        keywordField("owner"),
        keywordField("users"),
        textField("typeName"),
        keywordField("updatedBy"),
        keywordField("license"),
        keywordField("authors"),
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
        languageValuesMapping("title", keepRaw = true) ++
          languageValuesMapping("content", keepRaw = true) ++
          languageValuesMapping("tags")

      properties(fields ++ dynamics)

    }
  }

}

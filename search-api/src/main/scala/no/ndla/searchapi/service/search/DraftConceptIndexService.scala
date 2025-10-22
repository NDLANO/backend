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
import no.ndla.searchapi.integration.{DraftConceptApiClient, GrepApiClient, SearchApiClient}
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.common.model.domain.concept.Concept
import no.ndla.network.clients.{MyNDLAApiClient, TaxonomyApiClient}
import no.ndla.search.{NdlaE4sClient, SearchLanguage}

import scala.util.Try

class DraftConceptIndexService(using
    searchConverterService: SearchConverterService,
    draftConceptApiClient: DraftConceptApiClient,
    props: Props,
    e4sClient: NdlaE4sClient,
    searchLanguage: SearchLanguage,
    taxonomyApiClient: TaxonomyApiClient,
    grepApiClient: GrepApiClient,
    myNDLAApiClient: MyNDLAApiClient,
) extends IndexService[Concept]
    with StrictLogging {
  override val documentType: String                = "concept"
  override val searchIndex: String                 = props.SearchIndex(SearchType.Concepts)
  override val apiClient: SearchApiClient[Concept] = draftConceptApiClient

  override def createIndexRequest(
      domainModel: Concept,
      indexName: String,
      indexingBundle: IndexingBundle,
  ): Try[Option[IndexRequest]] = {
    searchConverterService
      .asSearchableConcept(domainModel, indexingBundle)
      .map { searchable =>
        val source = CirceUtil.toJsonString(searchable)
        Some(
          indexInto(indexName)
            .doc(source)
            .id(domainModel.id.get.toString)
            .versionType(EXTERNAL_GTE)
            .version(domainModel.revision.map(_.toLong).get)
        )
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
      ObjectField("responsible", properties = Seq(keywordField("responsibleId"), dateField("lastUpdated"))),
      textField("gloss"),
      longField("favorited"),
      ObjectField("domainObject", enabled = Some(false)),
    )
    val dynamics = languageValuesMapping("title", keepRaw = true) ++
      languageValuesMapping("content", keepRaw = true) ++
      languageValuesMapping("tags")

    properties(fields ++ dynamics)

  }
}

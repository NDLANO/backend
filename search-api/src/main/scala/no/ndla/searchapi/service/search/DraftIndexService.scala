/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
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
import no.ndla.common.model.domain.draft.Draft
import no.ndla.network.clients.{MyNDLAApiClient, TaxonomyApiClient}
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.{DraftApiClient, GrepApiClient, SearchApiClient}
import no.ndla.searchapi.model.domain.IndexingBundle

import scala.util.Try

class DraftIndexService(using
    searchConverterService: SearchConverterService,
    draftApiClient: DraftApiClient,
    props: Props,
    e4sClient: NdlaE4sClient,
    taxonomyApiClient: TaxonomyApiClient,
    grepApiClient: GrepApiClient,
    myNDLAApiClient: MyNDLAApiClient,
    searchLanguage: SearchLanguage,
) extends StrictLogging
    with IndexService[Draft] {
  override val documentType: String              = "draft"
  override val searchIndex: String               = props.SearchIndex(SearchType.Drafts)
  override val apiClient: SearchApiClient[Draft] = draftApiClient

  override def createIndexRequest(
      domainModel: Draft,
      indexName: String,
      indexingBundle: IndexingBundle,
  ): Try[Option[IndexRequest]] = {
    searchConverterService
      .asSearchableDraft(domainModel, indexingBundle)
      .map { searchableDraft =>
        val source = CirceUtil.toJsonString(searchableDraft)
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
      ObjectField("domainObject", enabled = Some(false)),
      intField("id"),
      keywordField("draftStatus.current"),
      keywordField("draftStatus.other"),
      keywordField("status"),
      keywordField("owner"),
      dateField("lastUpdated"),
      dateField("published"),
      keywordField("license"),
      keywordField("defaultTitle"),
      textField("typeName"),
      textField("authors"),
      keywordField("articleType"),
      keywordField("supportedLanguages"),
      textField("notes"),
      textField("previousVersionsNotes"),
      keywordField("users"),
      keywordField("grepContexts.code"),
      keywordField("grepContexts.status"),
      textField("grepContexts.title"),
      keywordField("traits"),
      longField("favorited"),
      keywordField("learningResourceType"),
      ObjectField("responsible", properties = Seq(keywordField("responsibleId"), dateField("lastUpdated"))),
      getTaxonomyContextMapping("context"),
      getTaxonomyContextMapping("contexts"),
      keywordField("contextids"),
      nestedField("embedResourcesAndIds").fields(
        keywordField("resource"),
        keywordField("id"),
        keywordField("language"),
      ),
      nestedField("metaImage").fields(keywordField("imageId"), keywordField("altText"), keywordField("language")),
      nestedField("revisionMeta").fields(
        keywordField("id"),
        dateField("revisionDate"),
        keywordField("note"),
        keywordField("status"),
      ),
      keywordField("nextRevision.id"),
      keywordField("nextRevision.status"),
      textField("nextRevision.note"),
      dateField("nextRevision.revisionDate"),
      keywordField("priority"),
      keywordField("defaultParentTopicName"),
      keywordField("defaultRoot"),
      keywordField("defaultResourceTypeName"),
    )
    val dynamics = languageValuesMapping("title", keepRaw = true) ++
      languageValuesMapping("metaDescription") ++
      languageValuesMapping("content") ++
      languageValuesMapping("introduction") ++
      languageValuesMapping("tags") ++
      languageValuesMapping("embedAttributes") ++
      languageValuesMapping("relevance") ++
      languageValuesMapping("breadcrumbs") ++
      languageValuesMapping("name", keepRaw = true) ++
      languageValuesMapping("parentTopicName", keepRaw = true) ++
      languageValuesMapping("resourceTypeName", keepRaw = true) ++
      languageValuesMapping("primaryRoot", keepRaw = true)

    properties(fields ++ dynamics)
  }
}

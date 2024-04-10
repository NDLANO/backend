/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
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
import no.ndla.common.model.domain.draft.Draft
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.DraftApiClient
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.search.SearchType

import scala.util.Try

trait DraftIndexService {
  this: SearchConverterService with IndexService with DraftApiClient with Props =>
  import props.SearchIndex
  val draftIndexService: DraftIndexService

  class DraftIndexService extends StrictLogging with IndexService[Draft] {
    override val documentType: String      = "draft"
    override val searchIndex: String       = SearchIndex(SearchType.Drafts)
    override val apiClient: DraftApiClient = draftApiClient

    override def createIndexRequest(
        domainModel: Draft,
        indexName: String,
        indexingBundle: IndexingBundle
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableDraft(domainModel, indexingBundle).map { searchableDraft =>
        val source = CirceUtil.toJsonString(searchableDraft)
        indexInto(indexName).doc(source).id(domainModel.id.get.toString)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        ObjectField("domainObject", enabled = Some(false)),
        intField("id"),
        keywordField("draftStatus.current"),
        keywordField("draftStatus.other"),
        dateField("lastUpdated"),
        dateField("published"),
        keywordField("license"),
        keywordField("defaultTitle"),
        textField("authors"),
        keywordField("articleType"),
        keywordField("supportedLanguages"),
        textField("notes"),
        textField("previousVersionsNotes"),
        keywordField("users"),
        keywordField("grepContexts.code"),
        textField("grepContexts.title"),
        keywordField("traits"),
        longField("favorited"),
        ObjectField(
          "responsible",
          properties = Seq(
            keywordField("responsibleId"),
            dateField("lastUpdated")
          )
        ),
        getTaxonomyContextMapping,
        nestedField("embedResourcesAndIds").fields(
          keywordField("resource"),
          keywordField("id"),
          keywordField("language")
        ),
        nestedField("metaImage").fields(
          keywordField("imageId"),
          keywordField("altText"),
          keywordField("language")
        ),
        nestedField("revisionMeta").fields(
          keywordField("id"),
          dateField("revisionDate"),
          keywordField("note"),
          keywordField("status")
        ),
        keywordField("nextRevision.id"),
        keywordField("nextRevision.status"),
        textField("nextRevision.note"),
        dateField("nextRevision.revisionDate"),
        keywordField("priority"),
        keywordField("defaultParentTopicName"),
        keywordField("defaultRoot"),
        keywordField("defaultResourceTypeName")
      )
      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("metaDescription") ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("visualElement") ++
        generateLanguageSupportedDynamicTemplates("introduction") ++
        generateLanguageSupportedDynamicTemplates("tags") ++
        generateLanguageSupportedDynamicTemplates("embedAttributes") ++
        generateLanguageSupportedDynamicTemplates("relevance") ++
        generateLanguageSupportedDynamicTemplates("breadcrumbs") ++
        generateLanguageSupportedDynamicTemplates("name", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("contexts.root", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("parentTopicName", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("resourceTypeName", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("primaryRoot", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("contexts.relevance") ++
        generateLanguageSupportedDynamicTemplates("contexts.resourceTypes.name")

      properties(fields).dynamicTemplates(dynamics)

    }
  }

}

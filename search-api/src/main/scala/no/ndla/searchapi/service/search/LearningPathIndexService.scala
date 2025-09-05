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
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.{GrepApiClient, LearningPathApiClient, SearchApiClient, TaxonomyApiClient}
import no.ndla.searchapi.model.domain.IndexingBundle

import scala.util.Try

class LearningPathIndexService(using
    searchConverterService: SearchConverterService,
    learningPathApiClient: LearningPathApiClient,
    props: Props,
    e4sClient: NdlaE4sClient,
    searchLanguage: SearchLanguage,
    taxonomyApiClient: TaxonomyApiClient,
    grepApiClient: GrepApiClient,
    myNDLAApiClient: MyNDLAApiClient
) extends StrictLogging
    with IndexService[LearningPath] {
  override val documentType: String                     = "learningpath"
  override val searchIndex: String                      = props.SearchIndex(SearchType.LearningPaths)
  override val apiClient: SearchApiClient[LearningPath] = learningPathApiClient

  override def createIndexRequest(
      domainModel: LearningPath,
      indexName: String,
      indexingBundle: IndexingBundle
  ): Try[IndexRequest] = {
    searchConverterService.asSearchableLearningPath(domainModel, indexingBundle).map { searchableLearningPath =>
      val source = CirceUtil.toJsonString(searchableLearningPath)
      indexInto(indexName)
        .doc(source)
        .id(domainModel.id.get.toString)
        .versionType(EXTERNAL_GTE)
        .version(domainModel.revision.map(_.toLong).get)
    }
  }

  def getMapping: MappingDefinition = {
    val fields = List(
      ObjectField("domainObject", enabled = Some(false)),
      intField("id"),
      textField("coverPhotoId"),
      intField("duration"),
      keywordField("learningResourceType"),
      keywordField("status"),
      keywordField("draftStatus.current"),
      keywordField("draftStatus.other"),
      keywordField("owner"),
      keywordField("users"),
      textField("verificationStatus"),
      dateField("lastUpdated"),
      keywordField("defaultTitle"),
      textField("authors"),
      keywordField("license"),
      longField("favorited"),
      nestedField("learningsteps").fields(
        textField("stepType")
      ),
      nestedField("revisionMeta").fields(
        keywordField("id"),
        dateField("revisionDate"),
        keywordField("note"),
        keywordField("status")
      ),
      ObjectField(
        "copyright",
        properties = Seq(
          ObjectField(
            "license",
            properties = Seq(
              textField("license"),
              textField("description"),
              textField("url")
            )
          ),
          nestedField("contributors").fields(
            textField("type"),
            textField("name")
          )
        )
      ),
      ObjectField(
        "responsible",
        properties = Seq(
          keywordField("responsibleId"),
          dateField("lastUpdated")
        )
      ),
      intField("isBasedOn"),
      keywordField("supportedLanguages"),
      getTaxonomyContextMapping("context"),
      getTaxonomyContextMapping("contexts"),
      keywordField("contextids"),
      nestedField("embedResourcesAndIds").fields(
        keywordField("resource"),
        keywordField("id"),
        keywordField("language")
      ),
      keywordField("nextRevision.id"),
      keywordField("nextRevision.status"),
      textField("nextRevision.note"),
      dateField(
        "nextRevision.revisionDate"
      ),
      keywordField("priority"),
      keywordField("defaultParentTopicName"),
      keywordField("defaultRoot"),
      keywordField("defaultResourceTypeName")
    )
    val dynamics =
      languageValuesMapping("title", keepRaw = true) ++
        languageValuesMapping("content") ++
        languageValuesMapping("description") ++
        languageValuesMapping("tags", keepRaw = true) ++
        languageValuesMapping("relevance") ++
        languageValuesMapping("breadcrumbs") ++
        languageValuesMapping("name", keepRaw = true) ++
        languageValuesMapping("parentTopicName", keepRaw = true) ++
        languageValuesMapping("resourceTypeName", keepRaw = true) ++
        languageValuesMapping("primaryRoot", keepRaw = true)

    properties(fields ++ dynamics)
  }
}

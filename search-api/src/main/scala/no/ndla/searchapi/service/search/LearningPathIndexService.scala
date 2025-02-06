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
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.LearningPathApiClient
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.search.SearchType

import scala.util.Try

trait LearningPathIndexService {
  this: SearchConverterService & IndexService & LearningPathApiClient & Props =>
  import props.SearchIndex
  val learningPathIndexService: LearningPathIndexService

  class LearningPathIndexService extends StrictLogging with IndexService[LearningPath] {
    override val documentType: String             = "learningpath"
    override val searchIndex: String              = SearchIndex(SearchType.LearningPaths)
    override val apiClient: LearningPathApiClient = learningPathApiClient

    override def createIndexRequest(
        domainModel: LearningPath,
        indexName: String,
        indexingBundle: IndexingBundle
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableLearningPath(domainModel, indexingBundle).map { searchableLearningPath =>
        val source = CirceUtil.toJsonString(searchableLearningPath)
        indexInto(indexName).doc(source).id(domainModel.id.get.toString)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        intField("id"),
        textField("coverPhotoId"),
        intField("duration"),
        keywordField("learningResourceType"),
        textField("status"),
        textField("verificationStatus"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        textField("authors"),
        keywordField("license"),
        longField("favorited"),
        nestedField("learningsteps").fields(
          textField("stepType")
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
        intField("isBasedOn"),
        keywordField("supportedLanguages"),
        getTaxonomyContextMapping,
        keywordField("contextids"),
        nestedField("embedResourcesAndIds").fields(
          keywordField("resource"),
          keywordField("id"),
          keywordField("language")
        ),
        dateField("nextRevision.revisionDate") // This is needed for sorting, even if it is never used for learningpaths
      )
      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("description") ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("relevance") ++
        generateLanguageSupportedDynamicTemplates("breadcrumbs") ++
        generateLanguageSupportedDynamicTemplates("name", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("contexts.root") ++
        generateLanguageSupportedDynamicTemplates("contexts.relevance") ++
        generateLanguageSupportedDynamicTemplates("contexts.resourceTypes.name")

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}

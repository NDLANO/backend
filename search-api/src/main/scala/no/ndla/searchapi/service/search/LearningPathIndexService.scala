/*
 * Part of NDLA search-api.
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
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.LearningPathApiClient
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle

import scala.util.Try

trait LearningPathIndexService {
  this: SearchConverterService with IndexService with LearningPathApiClient with Props =>
  val learningPathIndexService: LearningPathIndexService

  class LearningPathIndexService extends StrictLogging with IndexService[LearningPath] {
    override val documentType: String             = props.SearchDocuments(SearchType.LearningPaths)
    override val searchIndex: String              = props.SearchIndexes(SearchType.LearningPaths)
    override val apiClient: LearningPathApiClient = learningPathApiClient

    override def createIndexRequest(
        domainModel: LearningPath,
        indexName: String,
        taxonomyBundle: Option[TaxonomyBundle],
        grepBundle: Option[GrepBundle]
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableLearningPath(domainModel, taxonomyBundle).map { searchableLearningPath =>
        val source = CirceUtil.toJsonString(searchableLearningPath)
        indexInto(indexName).doc(source).id(domainModel.id.get.toString)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        intField("id"),
        textField("coverPhotoId"),
        intField("duration"),
        textField("status"),
        textField("verificationStatus"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        textField("authors"),
        keywordField("license"),
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

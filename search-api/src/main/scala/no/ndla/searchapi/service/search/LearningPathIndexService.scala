/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.LearningPathApiClient
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.{SearchType, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle
import org.json4s.Formats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait LearningPathIndexService {
  this: SearchConverterService with IndexService with LearningPathApiClient =>
  val learningPathIndexService: LearningPathIndexService

  class LearningPathIndexService extends LazyLogging with IndexService[LearningPath] {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.LearningPaths)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.LearningPaths)
    override val apiClient: LearningPathApiClient = learningPathApiClient

    override def createIndexRequest(domainModel: LearningPath,
                                    indexName: String,
                                    taxonomyBundle: TaxonomyBundle,
                                    grepBundle: Option[GrepBundle]): Try[IndexRequest] = {
      searchConverterService.asSearchableLearningPath(domainModel, taxonomyBundle) match {
        case Success(searchableLearningPath) =>
          val source = write(searchableLearningPath)
          Success(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
        case Failure(ex) =>
          Failure(ex)
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
          List(
            textField("stepType")
          )
        ),
        objectField("copyright").fields(
          objectField("license").fields(
            textField("license"),
            textField("description"),
            textField("url")
          ),
          nestedField("contributors").fields(
            textField("type"),
            textField("name")
          )
        ),
        intField("isBasedOn"),
        keywordField("supportedLanguages"),
        getTaxonomyContextMapping,
        nestedField("embedResourcesAndIds").fields(
          keywordField("resource"),
          keywordField("id"),
          keywordField("language")
        )
      )
      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("description") ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("relevance") ++
        generateLanguageSupportedDynamicTemplates("subject", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("breadcrumbs") ++
        generateLanguageSupportedDynamicTemplates("name", keepRaw = true)

      mapping(documentType).fields(fields).dynamicTemplates(dynamics)
    }
  }

}

/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.fields.ObjectField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.LearningPathApiClient
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle
import org.json4s.Formats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait LearningPathIndexService {
  this: SearchConverterService with IndexService with LearningPathApiClient with Props =>
  val learningPathIndexService: LearningPathIndexService

  class LearningPathIndexService extends StrictLogging with IndexService[LearningPath] {
    implicit val formats: Formats                 = SearchableLanguageFormats.JSonFormatsWithMillis
    override val documentType: String             = props.SearchDocuments(SearchType.LearningPaths)
    override val searchIndex: String              = props.SearchIndexes(SearchType.LearningPaths)
    override val apiClient: LearningPathApiClient = learningPathApiClient

    override def createIndexRequest(
        domainModel: LearningPath,
        indexName: String,
        taxonomyBundle: TaxonomyBundle,
        grepBundle: Option[GrepBundle]
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableLearningPath(domainModel, taxonomyBundle) match {
        case Success(searchableLearningPath) =>
          val source = Try(write(searchableLearningPath))
          source.map(s => indexInto(indexName).doc(s).id(domainModel.id.get.toString))
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
        generateLanguageSupportedDynamicTemplates("subject", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("breadcrumbs") ++
        generateLanguageSupportedDynamicTemplates("name", keepRaw = true)

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}

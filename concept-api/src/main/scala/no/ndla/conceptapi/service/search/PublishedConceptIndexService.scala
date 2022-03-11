/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.model.api.ConceptMissingIdException
import no.ndla.conceptapi.model.domain.Concept
import no.ndla.conceptapi.repository.{PublishedConceptRepository, Repository}
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait PublishedConceptIndexService {
  this: IndexService with PublishedConceptRepository with SearchConverterService =>
  val publishedConceptIndexService: PublishedConceptIndexService

  class PublishedConceptIndexService extends LazyLogging with IndexService[Concept] {
    implicit val formats: Formats                = SearchableLanguageFormats.JSonFormats
    override val documentType: String            = ConceptApiProperties.ConceptSearchDocument
    override val searchIndex: String             = ConceptApiProperties.PublishedConceptSearchIndex
    override val repository: Repository[Concept] = publishedConceptRepository

    override def createIndexRequest(concept: Concept, indexName: String): Try[IndexRequest] = {
      concept.id match {
        case Some(id) =>
          val source = write(searchConverterService.asSearchableConcept(concept))
          Success(
            indexInto(indexName).doc(source).id(id.toString)
          )

        case _ => Failure(ConceptMissingIdException("Attempted to create index request for concept without an id."))
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        intField("id"),
        keywordField("defaultTitle").normalizer("lower"),
        keywordField("subjectIds"),
        nestedField("metaImage").fields(
          keywordField("imageId"),
          keywordField("altText"),
          keywordField("language")
        ),
        dateField("lastUpdated"),
        dateField("created"),
        longField("articleIds"),
        keywordField("license"),
        keywordField("source"),
        keywordField("origin"),
        nestedField("copyright").fields(
          nestedField("creators").fields(
            keywordField("type"),
            keywordField("name")
          ),
          nestedField("processors").fields(
            keywordField("type"),
            keywordField("name")
          ),
          nestedField("rightsholders").fields(
            keywordField("type"),
            keywordField("name")
          )
        ),
        nestedField("embedResourcesAndIds").fields(
          keywordField("resource"),
          keywordField("id"),
          keywordField("language")
        )
      )
      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = true)
      properties(fields).dynamicTemplates(dynamics)
    }

  }

}

/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.{nestedField, _}
import com.sksamuel.elastic4s.fields.{ElasticField, ObjectField}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api.ConceptMissingIdException
import no.ndla.conceptapi.model.domain.{Concept, DBConcept}
import no.ndla.conceptapi.repository.{DraftConceptRepository, Repository}
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait DraftConceptIndexService {
  this: IndexService with DraftConceptRepository with SearchConverterService with Props with DBConcept =>
  val draftConceptIndexService: DraftConceptIndexService

  class DraftConceptIndexService extends StrictLogging with IndexService[Concept] {
    implicit val formats: Formats                = SearchableLanguageFormats.JSonFormats ++ Concept.serializers
    override val documentType: String            = props.ConceptSearchDocument
    override val searchIndex: String             = props.DraftConceptSearchIndex
    override val repository: Repository[Concept] = draftConceptRepository

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
      val fields: Seq[ElasticField] = List(
        intField("id"),
        keywordField("conceptType"),
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
        keywordField("status.current"),
        keywordField("status.other"),
        keywordField("updatedBy"),
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
        ),
        ObjectField(
          "responsible",
          properties = Seq(
            keywordField("responsibleId"),
            dateField("lastUpdated")
          )
        ),
        textField("gloss"),
        ObjectField("domainObject", enabled = Some(false))
      )
      val dynamics: Seq[DynamicTemplateRequest] = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = true)

      properties(fields).dynamicTemplates(dynamics)
    }

  }

}

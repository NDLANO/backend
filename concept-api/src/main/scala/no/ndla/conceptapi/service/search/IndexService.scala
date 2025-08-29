/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.analysis.{Analysis, CustomNormalizer}
import com.sksamuel.elastic4s.fields.{ElasticField, ObjectField}
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api.ConceptMissingIdException
import no.ndla.conceptapi.repository.Repository
import no.ndla.search.model.domain.{BulkIndexResult, ElasticIndexingException, ReindexResult}
import no.ndla.search.{BaseIndexService, NdlaE4sClient, SearchLanguage}

import scala.util.{Failure, Success, Try}

abstract class IndexService(using
    e4sClient: NdlaE4sClient,
    props: Props,
    searchConverterService: SearchConverterService,
    searchLanguage: SearchLanguage
) extends BaseIndexService
    with StrictLogging {
  val repository: Repository[Concept]
  override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

  private val lowerNormalizer: CustomNormalizer =
    CustomNormalizer("lower", charFilters = List.empty, tokenFilters = List("lowercase"))

  override val analysis: Analysis =
    Analysis(
      analyzers = List(searchLanguage.NynorskLanguageAnalyzer),
      tokenFilters = searchLanguage.NynorskTokenFilters,
      normalizers = List(lowerNormalizer)
    )

  private def createIndexRequest(concept: Concept, indexName: String) = {
    concept.id match {
      case Some(id) =>
        val searchable = searchConverterService.asSearchableConcept(concept)
        val source     = CirceUtil.toJsonString(searchable)
        Success(
          indexInto(indexName).doc(source).id(id.toString)
        )

      case _ => Failure(ConceptMissingIdException("Attempted to create index request for concept without an id."))
    }
  }

  def indexDocument(imported: Concept): Try[Concept] = {
    for {
      _       <- createIndexIfNotExists()
      request <- createIndexRequest(imported, searchIndex)
      _       <- e4sClient.execute(request)
    } yield imported
  }

  def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = synchronized {
    indexDocumentsInBulk(numShards)(sendToElastic)
  }

  private def sendToElastic(indexName: String): Try[BulkIndexResult] = {
    for {
      ranges  <- getRanges
      indexed <- ranges.traverse { case (start, end) =>
        val toIndex = repository.documentsWithIdBetween(start, end)
        indexDocuments(toIndex, indexName).map(numIndexed => (numIndexed, toIndex.size))
      }
    } yield countIndexed(indexed)
  }

  private def getRanges: Try[List[(Long, Long)]] = {
    Try {
      val (minId, maxId) = repository.minMaxId
      Seq
        .range(minId, maxId + 1)
        .grouped(props.IndexBulkSize)
        .map(group => (group.head, group.last))
        .toList
    }
  }

  def indexDocuments(contents: Seq[Concept], indexName: String): Try[Int] = {
    if (contents.isEmpty) {
      Success(0)
    } else {
      val req                    = contents.map(content => createIndexRequest(content, indexName))
      val indexRequests          = req.collect { case Success(indexRequest) => indexRequest }
      val failedToCreateRequests = req.collect { case Failure(ex) => Failure(ex) }

      if (indexRequests.nonEmpty) {
        val response = e4sClient.execute {
          bulk(indexRequests)
        }

        response match {
          case Success(r) =>
            val numFailed = r.result.failures.size + failedToCreateRequests.size
            logger.info(s"Indexed ${contents.size} documents ($documentType). No of failed items: $numFailed")
            Success(contents.size - numFailed)
          case Failure(ex) => Failure(ex)
        }
      } else {
        logger.error(s"All ${contents.size} requests failed to be created.")
        Failure(ElasticIndexingException("No indexReqeusts were created successfully."))
      }
    }
  }

  private def generateLanguageSupportedFieldList(fieldName: String, keepRaw: Boolean = false): Seq[ElasticField] = {
    if (keepRaw) {
      searchLanguage.languageAnalyzers.map(langAnalyzer =>
        textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
          .analyzer(langAnalyzer.analyzer)
          .fields(
            keywordField("raw"),
            keywordField("lower").normalizer("lower")
          )
      )
    } else {
      searchLanguage.languageAnalyzers.map(langAnalyzer =>
        textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
          .analyzer(langAnalyzer.analyzer)
      )
    }
  }

  def findAllIndexes: Try[Seq[String]] = findAllIndexes(this.searchIndex)

  def getMapping: MappingDefinition = {
    val fields: Seq[ElasticField] = List(
      intField("id"),
      keywordField("conceptType"),
      keywordField("defaultTitle").normalizer("lower"),
      dateField("lastUpdated"),
      dateField("created"),
      keywordField("status.current"),
      keywordField("status.other"),
      keywordField("updatedBy"),
      keywordField("license"),
      keywordField("source"),
      keywordField("origin"),
      keywordField("defaultSortableSubject"),
      keywordField("defaultSortableConceptType"),
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
    val dynamics = generateLanguageSupportedFieldList("title", keepRaw = true) ++
      generateLanguageSupportedFieldList("content") ++
      generateLanguageSupportedFieldList("tags", keepRaw = true) ++
      generateLanguageSupportedFieldList("sortableConceptType", keepRaw = true)

    properties(fields ++ dynamics)
  }

}

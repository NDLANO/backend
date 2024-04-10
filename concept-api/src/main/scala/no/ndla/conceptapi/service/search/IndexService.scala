/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.analysis.{Analysis, CustomNormalizer}
import com.sksamuel.elastic4s.fields.{ElasticField, ObjectField}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.integration.TaxonomyApiClient
import no.ndla.conceptapi.integration.model.TaxonomyData
import no.ndla.conceptapi.model.api.{ConceptMissingIdException, ElasticIndexingException}
import no.ndla.conceptapi.model.domain.ReindexResult
import no.ndla.conceptapi.repository.Repository
import no.ndla.search.SearchLanguage.{NynorskLanguageAnalyzer, languageAnalyzers}
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient with BaseIndexService with Props with TaxonomyApiClient with SearchConverterService =>
  trait IndexService extends BaseIndexService with StrictLogging {
    val repository: Repository[Concept]
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

    val lowerNormalizer: CustomNormalizer =
      CustomNormalizer("lower", charFilters = List.empty, tokenFilters = List("lowercase"))

    override val analysis: Analysis =
      Analysis(
        analyzers = List(NynorskLanguageAnalyzer),
        tokenFilters = SearchLanguage.NynorskTokenFilters,
        normalizers = List(lowerNormalizer)
      )

    private def createIndexRequest(
        concept: Concept,
        indexName: String,
        taxonomyData: TaxonomyData
    ): Try[IndexRequest] = {
      concept.id match {
        case Some(id) =>
          val searchable = searchConverterService.asSearchableConcept(concept, taxonomyData)
          val source     = CirceUtil.toJsonString(searchable)
          Success(
            indexInto(indexName).doc(source).id(id.toString)
          )

        case _ => Failure(ConceptMissingIdException("Attempted to create index request for concept without an id."))
      }
    }

    def indexDocument(imported: Concept): Try[Concept] = {
      for {
        _            <- createIndexIfNotExists()
        taxonomyData <- if (imported.subjectIds.nonEmpty) taxonomyApiClient.getSubjects else Success(TaxonomyData.empty)
        request      <- createIndexRequest(imported, searchIndex, taxonomyData)
        _            <- e4sClient.execute(request)
      } yield imported
    }

    def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndexWithGeneratedName(numShards).flatMap(indexName => {
          val operations = for {
            numIndexed  <- sendToElastic(indexName)
            aliasTarget <- getAliasTarget
            _           <- updateAliasTarget(aliasTarget, indexName)
          } yield numIndexed

          operations match {
            case Failure(f) =>
              deleteIndexWithName(Some(indexName)): Unit
              Failure(f)
            case Success(totalIndexed) =>
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
          }
        })
      }
    }

    private def sendToElastic(indexName: String): Try[Int] = {
      for {
        taxonomyData <- taxonomyApiClient.getSubjects
        ranges       <- getRanges
        indexed <- ranges.traverse { case (start, end) =>
          val toIndex = repository.documentsWithIdBetween(start, end)
          indexDocuments(toIndex, indexName, taxonomyData)
        }
      } yield indexed.sum
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

    def indexDocuments(contents: Seq[Concept], indexName: String, taxonomyData: TaxonomyData): Try[Int] = {
      if (contents.isEmpty) {
        Success(0)
      } else {
        val req                    = contents.map(content => createIndexRequest(content, indexName, taxonomyData))
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

    def findAllIndexes: Try[Seq[String]] = findAllIndexes(this.searchIndex)

    private def findAllIndexes(indexName: String): Try[Seq[String]] = {
      val response = e4sClient.execute {
        getAliases()
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.toList.map { case (index, _) => index.name }.filter(_.startsWith(indexName)))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    /** Returns Sequence of DynamicTemplateRequest for a given field.
      *
      * @param fieldName
      *   Name of field in mapping.
      * @param keepRaw
      *   Whether to add a keywordField named raw. Usually used for sorting, aggregations or scripts.
      * @return
      *   Sequence of DynamicTemplateRequest for a field.
      */
    protected def generateLanguageSupportedDynamicTemplates(
        fieldName: String,
        keepRaw: Boolean = false
    ): Seq[DynamicTemplateRequest] = {
      val fields =
        if (keepRaw)
          List(
            keywordField("raw"),
            keywordField("lower").normalizer("lower")
          )
        else List.empty

      val languageTemplates = languageAnalyzers.map(languageAnalyzer => {
        val name = s"$fieldName.${languageAnalyzer.languageTag.toString()}"
        DynamicTemplateRequest(
          name = name,
          mapping = textField(name).analyzer(languageAnalyzer.analyzer).fields(fields),
          matchMappingType = Some("string"),
          pathMatch = Some(name)
        )
      })
      val catchAlltemplate = DynamicTemplateRequest(
        name = fieldName,
        mapping = textField(fieldName).analyzer("standard").fields(fields),
        matchMappingType = Some("string"),
        pathMatch = Some(s"$fieldName.*")
      )
      languageTemplates ++ Seq(catchAlltemplate)
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
      val dynamics: Seq[DynamicTemplateRequest] = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("sortableSubject", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("sortableConceptType", keepRaw = true)

      properties(fields).dynamicTemplates(dynamics)
    }

  }
}

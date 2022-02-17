/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analysis.{Analysis, CustomNormalizer}
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.ConceptApiProperties.ElasticSearchIndexMaxResultWindow
import no.ndla.conceptapi.model.api.ElasticIndexingException
import no.ndla.conceptapi.model.domain.{Concept, ReindexResult}
import no.ndla.conceptapi.repository.Repository
import no.ndla.search.SearchLanguage.{NynorskLanguageAnalyzer, languageAnalyzers}
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import cats.implicits._

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient with BaseIndexService =>

  trait IndexService[D <: Concept] extends BaseIndexService with LazyLogging {
    val repository: Repository[D]
    override val MaxResultWindowOption: Int = ElasticSearchIndexMaxResultWindow

    val lowerNormalizer: CustomNormalizer =
      CustomNormalizer("lower", charFilters = List.empty, tokenFilters = List("lowercase"))

    override val analysis: Analysis =
      Analysis(
        analyzers = List(NynorskLanguageAnalyzer),
        tokenFilters = SearchLanguage.NynorskTokenFilters,
        normalizers = List(lowerNormalizer)
      )

    def createIndexRequest(domainModel: D, indexName: String): Try[IndexRequest]

    def indexDocument(imported: D): Try[D] = {
      for {
        _       <- createIndexIfNotExists()
        request <- createIndexRequest(imported, searchIndex)
        _       <- e4sClient.execute(request)
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndexWithGeneratedName.flatMap(indexName => {
          val operations = for {
            numIndexed  <- sendToElastic(indexName)
            aliasTarget <- getAliasTarget
            _           <- updateAliasTarget(aliasTarget, indexName)
          } yield numIndexed

          operations match {
            case Failure(f) =>
              deleteIndexWithName(Some(indexName))
              Failure(f)
            case Success(totalIndexed) =>
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
          }
        })
      }
    }

    def sendToElastic(indexName: String): Try[Int] = {
      getRanges
        .flatMap(ranges => {
          ranges.traverse { case (start, end) =>
            val toIndex = repository.documentsWithIdBetween(start, end)
            indexDocuments(toIndex, indexName)
          }
        })
        .map(_.sum)
    }

    def getRanges: Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = repository.minMaxId
        Seq
          .range(minId, maxId + 1)
          .grouped(ConceptApiProperties.IndexBulkSize)
          .map(group => (group.head, group.last))
          .toList
      }
    }

    def indexDocuments(contents: Seq[D], indexName: String): Try[Int] = {
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

    /** Returns Sequence of FieldDefinitions for a given field.
      *
      * @param fieldName
      *   Name of field in mapping.
      * @param keepRaw
      *   Whether to add a keywordField named raw. Usually used for sorting, aggregations or scripts.
      * @return
      *   Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(
        fieldName: String,
        keepRaw: Boolean = false
    ): Seq[ElasticField] = {
      keepRaw match {
        case true =>
          languageAnalyzers.map(langAnalyzer =>
            textField(s"$fieldName.${langAnalyzer.languageTag.toString()}")
              .fielddata(false)
              .analyzer(langAnalyzer.analyzer)
              .fields(keywordField("raw"), keywordField("lower").normalizer("lower"))
          )
        case false =>
          languageAnalyzers.map(langAnalyzer =>
            textField(s"$fieldName.${langAnalyzer.languageTag.toString()}")
              .fielddata(false)
              .analyzer(langAnalyzer.analyzer)
          )
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
      val fields = new ListBuffer[ElasticField]()
      if (keepRaw) {
        fields += keywordField("raw") += keywordField("lower").normalizer("lower")
      }
      val languageTemplates = languageAnalyzers.map(languageAnalyzer => {
        val name = s"$fieldName.${languageAnalyzer.languageTag.toString()}"
        DynamicTemplateRequest(
          name = name,
          mapping = textField(name).analyzer(languageAnalyzer.analyzer).fields(fields.toList),
          matchMappingType = Some("string"),
          pathMatch = Some(name)
        )
      })
      val catchAlltemplate = DynamicTemplateRequest(
        name = fieldName,
        mapping = textField(fieldName).analyzer("standard").fields(fields.toList),
        matchMappingType = Some("string"),
        pathMatch = Some(s"$fieldName.*")
      )
      languageTemplates ++ Seq(catchAlltemplate)
    }

  }
}

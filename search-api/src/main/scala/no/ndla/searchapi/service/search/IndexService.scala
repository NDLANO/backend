/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.analysis.*
import com.sksamuel.elastic4s.fields.{ElasticField, NestedField}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import no.ndla.common.model.domain.Content
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.search.SearchLanguage.NynorskLanguageAnalyzer
import no.ndla.search.model.domain.{BulkIndexResult, ElasticIndexingException, ReindexResult}
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.model.domain.IndexingBundle

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient & SearchApiClient & BaseIndexService & TaxonomyApiClient & GrepApiClient & Props &
    MyNDLAApiClient =>

  trait BulkIndexingService extends BaseIndexService {

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
      val dynamicFunc = (name: String, analyzer: String, subFields: List[ElasticField]) => {
        val field = textField(name).analyzer(analyzer).fields(subFields)
        DynamicTemplateRequest(
          name = name,
          mapping = field,
          matchMappingType = Some("string"),
          pathMatch = Some(name)
        )
      }

      val sf = List(
        textField("trigram").analyzer("trigram"),
        textField("decompounded").searchAnalyzer("standard").analyzer("compound_analyzer"),
        textField("exact").analyzer("exact")
      )
      val subFields = if (keepRaw) sf :+ keywordField("raw") else sf

      val languageTemplates = SearchLanguage.languageAnalyzers.map(languageAnalyzer => {
        val name = s"$fieldName.${languageAnalyzer.languageTag.toString()}"
        dynamicFunc(name, languageAnalyzer.analyzer, subFields)
      })
      val languageSubTemplates = SearchLanguage.languageAnalyzers.map(languageAnalyzer => {
        val name = s"*.$fieldName.${languageAnalyzer.languageTag.toString()}"
        dynamicFunc(name, languageAnalyzer.analyzer, subFields)
      })
      val catchAllTemplate    = dynamicFunc(s"$fieldName.*", "standard", subFields)
      val catchAllSubTemplate = dynamicFunc(s"*.$fieldName.*", "standard", subFields)
      languageTemplates ++ languageSubTemplates ++ Seq(catchAllTemplate, catchAllSubTemplate)
    }

    private val hyphDecompounderTokenFilter: CompoundWordTokenFilter = CompoundWordTokenFilter(
      name = "hyphenation_decompounder",
      `type` = HyphenationDecompounder,
      wordListPath = Some("compound-words-norwegian-wordlist.txt"),
      hyphenationPatternsPath = Some("hyph/no.xml"),
      minSubwordSize = Some(4),
      onlyLongestMatch = Some(false)
    )

    private val customCompoundAnalyzer =
      CustomAnalyzer(
        "compound_analyzer",
        "whitespace",
        tokenFilters = List(hyphDecompounderTokenFilter.name)
      )

    private val customExactAnalyzer = CustomAnalyzer("exact", "whitespace")

    val shingle: ShingleTokenFilter =
      ShingleTokenFilter(name = "shingle", minShingleSize = Some(2), maxShingleSize = Some(3))

    val trigram: CustomAnalyzer =
      CustomAnalyzer(name = "trigram", tokenizer = "standard", tokenFilters = List("lowercase", "shingle"))

    val lowerNormalizer: CustomNormalizer =
      CustomNormalizer("lower", charFilters = List.empty, tokenFilters = List("lowercase"))

    override val analysis: Analysis =
      Analysis(
        analyzers = List(trigram, customExactAnalyzer, customCompoundAnalyzer, NynorskLanguageAnalyzer),
        tokenFilters = List(hyphDecompounderTokenFilter) ++ SearchLanguage.NynorskTokenFilters,
        normalizers = List(lowerNormalizer)
      )
  }

  trait IndexService[D <: Content] extends BulkIndexingService with StrictLogging {
    val apiClient: SearchApiClient
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

    def createIndexRequest(domainModel: D, indexName: String, indexingBundle: IndexingBundle): Try[IndexRequest]

    def indexDocument(imported: D): Try[D] = {
      val grepBundle = grepApiClient.getGrepBundle() match {
        case Success(bundle) => Some(bundle)
        case Failure(_) =>
          logger.error(
            s"GREP could not be fetched when indexing $documentType ${imported.id.map(id => s"with id: '$id'").getOrElse("")}"
          )
          None
      }

      val indexingBundle = IndexingBundle(grepBundle = grepBundle, taxonomyBundle = None, myndlaBundle = None)
      indexDocument(imported, indexingBundle)
    }

    def indexDocument(
        imported: D,
        indexingBundle: IndexingBundle
    ): Try[D] = {
      for {
        _       <- createIndexIfNotExists()
        request <- createIndexRequest(imported, searchIndex, indexingBundle)
        _       <- e4sClient.execute(request)
      } yield imported
    }
    def indexDocuments(shouldUsePublishedTax: Boolean)(implicit d: Decoder[D]): Try[ReindexResult] =
      indexDocuments(shouldUsePublishedTax, None)

    def indexDocuments(shouldUsePublishedTax: Boolean, numShards: Option[Int])(implicit
        d: Decoder[D]
    ): Try[ReindexResult] = {
      val bundles = for {
        taxonomyBundle <- taxonomyApiClient.getTaxonomyBundle(shouldUsePublishedTax)
        grepBundle     <- grepApiClient.getGrepBundle()
        myndlaBundle   <- myndlaApiClient.getMyNDLABundle
      } yield IndexingBundle(Some(grepBundle), Some(taxonomyBundle), Some(myndlaBundle))
      bundles match {
        case Failure(ex) =>
          logger.error(s"Grep and/or Taxonomy could not be fetched when reindexing all $documentType")
          Failure(ex)
        case Success(indexingBundle) =>
          indexDocuments(numShards, indexingBundle)
      }
    }

    def reindexDocument(id: Long)(implicit d: Decoder[D]): Try[D] = {
      for {
        grepBundle <- grepApiClient.getGrepBundle()
        indexingBundle = IndexingBundle(grepBundle = Some(grepBundle), None, None)
        _       <- createIndexIfNotExists()
        toIndex <- apiClient.getSingle[D](id)
        request <- createIndexRequest(toIndex, searchIndex, indexingBundle)
        _ <- e4sClient.execute {
          request
        }
      } yield toIndex
    }

    def indexDocuments(indexingBundle: IndexingBundle)(implicit d: Decoder[D]): Try[ReindexResult] =
      indexDocuments(None, indexingBundle)

    def indexDocuments(
        numShards: Option[Int],
        indexingBundle: IndexingBundle
    )(implicit
        d: Decoder[D]
    ): Try[ReindexResult] = {
      indexDocumentsInBulk(numShards) { indexName =>
        sendToElastic(indexName, indexingBundle)
      }
    }

    private def sendToElastic(indexName: String, indexingBundle: IndexingBundle)(implicit
        d: Decoder[D]
    ): Try[BulkIndexResult] = {

      val chunks = apiClient.getChunks[D]
      val results = chunks
        .map({
          case Failure(ex) => Failure(ex)
          case Success(c) =>
            indexDocuments(c, indexName, indexingBundle)
              .map(numIndexed => (numIndexed, c.size))
        })
        .toList

      results.collect { case Failure(ex) => Failure(ex) } match {
        case Nil =>
          val successfulChunks = results.collect { case Success((chunkIndexed, chunkSize)) =>
            (chunkIndexed, chunkSize)
          }

          val indexResult = countIndexed(successfulChunks)
          logger.info(
            s"${indexResult.count}/${indexResult.totalCount} documents ($documentType) were indexed successfully."
          )
          Success(indexResult)

        case notEmpty => notEmpty.head
      }
    }

    def indexDocuments(
        contents: Seq[D],
        indexName: String,
        indexingBundle: IndexingBundle
    ): Try[Int] = {
      if (contents.isEmpty) {
        Success(0)
      } else {
        val req = contents.map { content =>
          createIndexRequest(content, indexName, indexingBundle).recoverWith({ case ex =>
            logger.error(s"Failed to create indexRequest for $documentType with id: ${content.id}", ex)
            Failure(ex)
          })
        }

        val indexRequests          = req.collect { case Success(indexRequest) => indexRequest }
        val failedToCreateRequests = req.collect { case Failure(ex) => Failure(ex) }

        if (indexRequests.nonEmpty) {
          val response = e4sClient.execute {
            bulk(indexRequests)
          }

          response match {
            case Success(r) =>
              val numFailed = r.result.failures.size + failedToCreateRequests.size
              r.result.failures.foreach(failure => {
                logger.error(s"Received bulk error from elasticsearch: $failure")
              })

              logger.info(s"Indexed ${contents.size} documents ($documentType). No of failed items: $numFailed")
              Success(contents.size - numFailed)
            case Failure(ex) =>
              logger.error(s"Failed to index ${contents.size} documents ($documentType): ${ex.getMessage}", ex)
              Failure(ex)
          }
        } else {
          logger.error(s"All ${contents.size} requests failed to be created.")
          Failure(ElasticIndexingException("No indexRequests were created successfully."))
        }
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
      SearchLanguage.languageAnalyzers.map(langAnalyzer => {
        val sf = List(
          textField("trigram").analyzer("trigram"),
          textField("decompounded")
            .searchAnalyzer("standard")
            .analyzer("compound_analyzer"),
          textField("exact")
            .analyzer("exact")
        )

        val subFields = if (keepRaw) sf :+ keywordField("raw") else sf

        textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
          .analyzer(langAnalyzer.analyzer)
          .fields(subFields)
      })
    }

    protected def getTaxonomyContextMapping: NestedField = {
      nestedField("contexts").fields(
        keywordField("publicId"),
        keywordField("contextId"),
        keywordField("path"),
        keywordField("contextType"),
        keywordField("rootId"),
        keywordField("parentIds"),
        keywordField("relevanceId"),
        booleanField("isActive"),
        booleanField("isPrimary"),
        keywordField("url"),
        nestedField("resourceTypes").fields(
          keywordField("id")
        )
      )
    }

  }

}

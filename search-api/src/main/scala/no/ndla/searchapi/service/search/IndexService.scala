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
import com.sksamuel.elastic4s.fields.{ElasticField, NestedField, ObjectField}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import no.ndla.common.model.domain.Content
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.search.model.domain.{BulkIndexResult, ElasticIndexingException, ReindexResult}
import no.ndla.search.{BaseIndexService, NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.model.domain.IndexingBundle

import scala.util.{Failure, Success, Try}

abstract class BulkIndexingService(using props: Props, searchLanguage: SearchLanguage, e4sClient: NdlaE4sClient)
    extends BaseIndexService {

  protected def languageValuesMapping(name: String, keepRaw: Boolean = false): Seq[ElasticField] = {
    val subfields = List(
      textField("trigram").analyzer("trigram"),
      textField("decompounded").searchAnalyzer("standard").analyzer("compound_analyzer"),
      textField("exact").analyzer("exact")
    ) ++
      Option.when(keepRaw)(keywordField("raw")).toList

    val analyzedFields = searchLanguage.languageAnalyzers.map(langAnalyzer => {
      textField(s"$name.${langAnalyzer.languageTag.toString}")
        .analyzer(langAnalyzer.analyzer)
        .fields(subfields)
    })

    analyzedFields
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

  private val lowerNormalizer: CustomNormalizer =
    CustomNormalizer("lower", charFilters = List.empty, tokenFilters = List("lowercase"))

  override val analysis: Analysis =
    Analysis(
      analyzers = List(trigram, customExactAnalyzer, customCompoundAnalyzer, searchLanguage.NynorskLanguageAnalyzer),
      tokenFilters = List(hyphDecompounderTokenFilter) ++ searchLanguage.NynorskTokenFilters,
      normalizers = List(lowerNormalizer)
    )

  protected def getTaxonomyContextMapping(fieldName: String): NestedField = {
    nestedField(fieldName).fields(
      List(
        ObjectField("domainObject", enabled = Some(false)),
        keywordField("publicId"),
        keywordField("contextId"),
        keywordField("path"),
        keywordField("contextType"),
        keywordField("rootId"),
        keywordField("parentIds"),
        keywordField("relevanceId"),
        booleanField("isActive"),
        booleanField("isPrimary"),
        booleanField("isVisible"),
        keywordField("url"),
        keywordField("resourceTypeIds")
      ) ++
        languageValuesMapping("breadcrumbs", keepRaw = true)
    )
  }
}

trait IndexService[D <: Content](using
    props: Props,
    e4sClient: NdlaE4sClient,
    taxonomyApiClient: TaxonomyApiClient,
    grepApiClient: GrepApiClient,
    myNDLAApiClient: MyNDLAApiClient
) extends BulkIndexingService
    with StrictLogging {
  val apiClient: SearchApiClient[D]
  override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

  def createIndexRequest(domainModel: D, indexName: String, indexingBundle: IndexingBundle): Try[Option[IndexRequest]]

  def indexDocument(imported: D): Try[D] = {
    val grepBundle = grepApiClient.getGrepBundle() match {
      case Success(bundle) => Some(bundle)
      case Failure(_)      =>
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
      _            <- createIndexIfNotExists()
      maybeRequest <- createIndexRequest(imported, searchIndex, indexingBundle)
      _ = maybeRequest.map(e4sClient.execute(_))
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
      myndlaBundle   <- myNDLAApiClient.getMyNDLABundle
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
      _            <- createIndexIfNotExists()
      toIndex      <- apiClient.getSingle(id)
      maybeRequest <- createIndexRequest(toIndex, searchIndex, indexingBundle)
      _ = maybeRequest.map(e4sClient.execute(_))
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

    val chunks  = apiClient.getChunks
    val results = chunks
      .map({
        case Failure(ex) =>
          logger.error(s"Failed to fetch chunk from with api client '${apiClient.name}'", ex)
          Failure(ex)
        case Success(c) =>
          indexDocuments(c, indexName, indexingBundle).map(numIndexed => (numIndexed, c.size))
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
          bulk(indexRequests.filter(_.isDefined).flatten)
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
}

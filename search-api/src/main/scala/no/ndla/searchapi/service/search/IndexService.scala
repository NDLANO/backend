/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analysis._
import com.sksamuel.elastic4s.fields.{ElasticField, NestedField}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.Content
import no.ndla.search.SearchLanguage.NynorskLanguageAnalyzer
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ElasticIndexingException
import no.ndla.searchapi.model.domain.ReindexResult
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle

import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient
    with SearchApiClient
    with BaseIndexService
    with TaxonomyApiClient
    with GrepApiClient
    with Props =>

  trait IndexService[D <: Content] extends BaseIndexService with StrictLogging {
    val apiClient: SearchApiClient
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

    def createIndexRequest(
        domainModel: D,
        indexName: String,
        taxonomyBundle: Option[TaxonomyBundle],
        grepBundle: Option[GrepBundle]
    ): Try[IndexRequest]

    def indexDocument(imported: D): Try[D] = {
      val grepBundle = grepApiClient.getGrepBundle() match {
        case Success(bundle) => Some(bundle)
        case Failure(_) =>
          logger.error(
            s"GREP could not be fetched when indexing $documentType ${imported.id.map(id => s"with id: '$id'").getOrElse("")}"
          )
          None
      }

      indexDocument(imported, None, grepBundle)
    }

    def indexDocument(imported: D, taxonomyBundle: Option[TaxonomyBundle], grepBundle: Option[GrepBundle]): Try[D] = {
      for {
        _       <- createIndexIfNotExists()
        request <- createIndexRequest(imported, searchIndex, taxonomyBundle, grepBundle)
        _ <- e4sClient.execute {
          request
        }
      } yield imported
    }

    def indexDocuments()(implicit mf: Manifest[D]): Try[ReindexResult] = {
      val bundles = for {
        taxonomyBundle <- taxonomyApiClient.getTaxonomyBundle()
        grepBundle     <- grepApiClient.getGrepBundle()
      } yield (taxonomyBundle, grepBundle)
      bundles match {
        case Failure(ex) =>
          logger.error(s"Grep and/or Taxonomy could not be fetched when reindexing all $documentType")
          Failure(ex)
        case Success((taxonomyBundle, grepBundle)) => indexDocuments(taxonomyBundle, grepBundle)
      }
    }

    def reindexDocument(id: Long)(implicit mf: Manifest[D]): Try[D] = {
      for {
        grepBundle <- grepApiClient.getGrepBundle()
        _          <- createIndexIfNotExists()
        toIndex    <- apiClient.getSingle[D](id)
        request    <- createIndexRequest(toIndex, searchIndex, None, Some(grepBundle))
        _ <- e4sClient.execute {
          request
        }
      } yield toIndex
    }

    def indexDocuments(taxonomyBundle: TaxonomyBundle, grepBundle: GrepBundle)(implicit
        mf: Manifest[D]
    ): Try[ReindexResult] = {
      val start = System.currentTimeMillis()
      createIndexWithGeneratedName.flatMap(indexName => {
        sendToElastic(indexName, taxonomyBundle, grepBundle) match {
          case Failure(ex) =>
            deleteIndexWithName(Some(indexName))
            Failure(ex)
          case Success((count, totalCount)) =>
            val numErrors = totalCount - count

            if (numErrors > 0) {
              logger.error(s"Indexing completed, but with $numErrors errors.")
              deleteIndexWithName(Some(indexName))
              Failure(ElasticIndexingException(s"Indexing completed with $numErrors errors, will not replace index."))
            } else {
              val operations = getAliasTarget.flatMap(updateAliasTarget(_, indexName))
              operations.map(_ =>
                ReindexResult(
                  documentType,
                  numErrors,
                  count,
                  System.currentTimeMillis() - start
                )
              )
            }
        }
      })
    }

    def sendToElastic(
        indexName: String,
        taxonomyBundle: TaxonomyBundle,
        grepBundle: GrepBundle
    )(implicit mf: Manifest[D]): Try[(Int, Int)] = {
      implicit val executionContext: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(3))

      val stream = apiClient.getChunks[D]
      val futures = stream
        .map(_.flatMap {
          case Failure(ex) => Future.successful(Failure(ex))
          case Success(c) =>
            indexDocuments(c, indexName, taxonomyBundle, grepBundle).map(numIndexed =>
              Success((numIndexed.getOrElse(0), c.size))
            )

        })
        .toList

      val chunks = Await.result(Future.sequence(futures), Duration.Inf)
      executionContext.shutdown()

      chunks.collect { case Failure(ex) => Failure(ex) } match {
        case Nil =>
          val successfulChunks = chunks.collect { case Success((chunkIndexed, chunkSize)) =>
            (chunkIndexed, chunkSize)
          }

          val (count, totalCount) = successfulChunks.foldLeft((0, 0)) {
            case ((totalIndexed, totalSize), (chunkIndexed, chunkSize)) =>
              (totalIndexed + chunkIndexed, totalSize + chunkSize)
          }

          logger.info(s"$count/$totalCount documents ($documentType) were indexed successfully.")
          Success((count, totalCount))

        case notEmpty => notEmpty.head
      }
    }

    def indexDocuments(contents: Seq[D], indexName: String, taxonomyBundle: TaxonomyBundle, grepBundle: GrepBundle)(
        implicit ec: ExecutionContext
    ): Future[Try[Int]] = {
      if (contents.isEmpty) {
        Future.successful { Success(0) }
      } else {
        val req = contents.map(content => {
          createIndexRequest(content, indexName, Some(taxonomyBundle), Some(grepBundle))
        })
        val indexRequests          = req.collect { case Success(indexRequest) => indexRequest }
        val failedToCreateRequests = req.collect { case Failure(ex) => Failure(ex) }

        Future {
          if (indexRequests.nonEmpty) {
            val response = e4sClient.execute {
              bulk(indexRequests)
            }

            response match {
              case Success(r) =>
                val numFailed = r.result.failures.size + failedToCreateRequests.size
                logger.info(s"Indexed ${contents.size} documents ($documentType). No of failed items: $numFailed")
                Success(contents.size - numFailed)
              case Failure(ex) =>
                logger.error(s"Failed to index ${contents.size} documents ($documentType): ${ex.getMessage}", ex)
                Failure(ex)
            }
          } else {
            logger.error(s"All ${contents.size} requests failed to be created.")
            Failure(ElasticIndexingException("No indexReqeusts were created successfully."))
          }
        }
      }
    }

    val hyphDecompounderTokenFilter: CompoundWordTokenFilter = CompoundWordTokenFilter(
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

    override val analysis: Analysis =
      Analysis(
        analyzers = List(trigram, customExactAnalyzer, customCompoundAnalyzer, NynorskLanguageAnalyzer),
        tokenFilters = List(hyphDecompounderTokenFilter) ++ SearchLanguage.NynorskTokenFilters
      )

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
        DynamicTemplateRequest(
          name = name,
          mapping = textField(name).analyzer(analyzer).fields(subFields),
          matchMappingType = Some("string"),
          pathMatch = Some(name)
        )
      }

      val sf = List(
        textField("trigram").analyzer("trigram"),
        textField("decompounded")
          .searchAnalyzer("standard")
          .analyzer("compound_analyzer"),
        textField("exact")
          .analyzer("exact")
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

    protected def getTaxonomyContextMapping: NestedField = {
      nestedField("contexts").fields(
        List(
          keywordField("id"),
          keywordField("path"),
          keywordField("contextType"),
          keywordField("subjectId"),
          keywordField("parentTopicIds"),
          keywordField("relevanceId"),
          nestedField("resourceTypes").fields(
            List(keywordField("id"))
          )
        )
      )
    }

  }

}

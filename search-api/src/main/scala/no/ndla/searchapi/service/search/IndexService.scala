/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.fields.{ElasticField, NestedField}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analysis.{
  Analysis,
  CompoundWordTokenFilter,
  CustomAnalyzer,
  HyphenationDecompounder,
  ShingleTokenFilter
}
import com.sksamuel.elastic4s.requests.alias.AliasAction
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.search.Elastic4sClient
import no.ndla.search.Language.NynorskLanguageAnalyzer
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ElasticIndexingException
import no.ndla.search.Language
import no.ndla.searchapi.model.domain.{Content, ReindexResult}
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient with SearchApiClient with TaxonomyApiClient with GrepApiClient =>

  trait IndexService[D <: Content] extends LazyLogging {
    val apiClient: SearchApiClient
    val documentType: String
    val searchIndex: String

    // Setting this to suppress the warning, since it will default to '1' in the new version.
    // The value '5' was chosen since that is what was the default earlier (which worked fine).
    // However there are probably more optimal values.
    val indexShards: Int = 5

    def getMapping: MappingDefinition

    def createIndexRequest(domainModel: D,
                           indexName: String,
                           taxonomyBundle: TaxonomyBundle,
                           grepBundle: Option[GrepBundle]): Try[IndexRequest]

    def indexDocument(imported: D): Try[D] = {
      val taxonomyBundleT = taxonomyApiClient.getTaxonomyBundle()

      val grepBundle = grepApiClient.getGrepBundle() match {
        case Success(bundle) => Some(bundle)
        case Failure(_) =>
          logger.error(
            s"GREP could not be fetched when indexing $documentType ${imported.id.map(id => s"with id: '$id'").getOrElse("")}")
          None
      }

      taxonomyBundleT match {
        case Failure(ex) =>
          logger.error(
            s"Taxonomy could not be fetched when indexing $documentType ${imported.id.map(id => s"with id: '$id'").getOrElse("")}",
            ex)
          Failure(ex)
        case Success(taxonomyBundle) => indexDocument(imported, taxonomyBundle, grepBundle)
      }
    }

    def indexDocument(imported: D, taxonomyBundle: TaxonomyBundle, grepBundle: Option[GrepBundle]): Try[D] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None        => createIndexAndAlias()
        }
        request <- createIndexRequest(imported, searchIndex, taxonomyBundle, grepBundle)
        _ <- e4sClient.execute {
          request
        }
      } yield imported
    }

    def indexDocuments()(implicit mf: Manifest[D]): Try[ReindexResult] = {
      val bundles = for {
        taxonomyBundle <- taxonomyApiClient.getTaxonomyBundle()
        grepBundle <- grepApiClient.getGrepBundle()
      } yield (taxonomyBundle, grepBundle)
      bundles match {
        case Failure(ex) =>
          logger.error(s"Grep and/or Taxonomy could not be fetched when reindexing all $documentType")
          Failure(ex)
        case Success((taxonomyBundle, grepBundle)) => indexDocuments(taxonomyBundle, grepBundle)
      }
    }

    def indexDocuments(taxonomyBundle: TaxonomyBundle, grepBundle: GrepBundle)(
        implicit mf: Manifest[D]): Try[ReindexResult] = {
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
              Failure(ElasticIndexingException(s"Indexing completed with $numErrors, will not replace index."))
            } else {
              val operations = getAliasTarget.flatMap(updateAliasTarget(_, indexName))
              operations.map(
                _ =>
                  ReindexResult(
                    documentType,
                    numErrors,
                    count,
                    System.currentTimeMillis() - start
                ))
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
              Success(numIndexed.getOrElse(0), c.size))

        })
        .toList

      val chunks = Await.result(Future.sequence(futures), Duration.Inf)
      executionContext.shutdown()

      chunks.collect { case Failure(ex) => Failure(ex) } match {
        case Nil =>
          val successfulChunks = chunks.collect {
            case Success((chunkIndexed, chunkSize)) =>
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
        implicit ec: ExecutionContext): Future[Try[Int]] = {
      if (contents.isEmpty) {
        Future.successful { Success(0) }
      } else {
        val req = contents.map(content => createIndexRequest(content, indexName, taxonomyBundle, Some(grepBundle)))
        val indexRequests = req.collect { case Success(indexRequest) => indexRequest }
        val failedToCreateRequests = req.collect { case Failure(ex)  => Failure(ex) }

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

    def deleteDocument(contentId: Long): Try[_] = {
      for {
        _ <- getAliasTarget.map {
          case Some(index) => Success(index)
          case None        => createIndexAndAlias()
        }
        deleted <- {
          e4sClient.execute {
            deleteById(searchIndex, s"$contentId")
          }
        }
      } yield deleted
    }

    def createIndexAndAlias(): Try[String] = {
      createIndexWithGeneratedName.map(newIndex => {
        updateAliasTarget(None, newIndex)
        newIndex
      })
    }

    def createIndexWithGeneratedName: Try[String] = createIndexWithName(searchIndex + "_" + getTimestamp)

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

    val analysis: Analysis = Analysis(
      analyzers = List(trigram, customExactAnalyzer, customCompoundAnalyzer, NynorskLanguageAnalyzer),
      tokenFilters = List(hyphDecompounderTokenFilter) ++ Language.tokenFilters
    )

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          createIndex(indexName)
            .shards(indexShards)
            .mapping(getMapping)
            .analysis(analysis)
            .indexSetting("max_result_window", SearchApiProperties.ElasticSearchIndexMaxResultWindow)
            .replicas(0)
        }

        response match {
          case Success(_)  => Success(indexName)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def getAliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute {
        getAliases(Nil, List(searchIndex))
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.headOption.map(t => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateReplicaNumber(indexName: String): Try[_] = {
      e4sClient.execute {
        updateSettings(Indexes(indexName), Map("number_of_replicas" -> "1"))
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = synchronized {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val actions = oldIndexName match {
          case None =>
            List[AliasAction](addAlias(searchIndex, newIndexName))
          case Some(oldIndex) =>
            List[AliasAction](removeAlias(searchIndex, oldIndex), addAlias(searchIndex, newIndexName))
        }

        e4sClient.execute(aliases(actions)) match {
          case Success(_) =>
            logger.info("Alias target updated successfully, deleting other indexes.")
            for {
              _ <- cleanupIndexes()
              _ <- updateReplicaNumber(newIndexName)
            } yield ()
          case Failure(ex) =>
            logger.error("Could not update alias target.")
            Failure(ex)
        }

      }
    }

    /**
      * Deletes every index that is not in use by this indexService.
      * Only indexes starting with indexName are deleted.
      *
      * @param indexName Start of index names that is deleted if not aliased.
      * @return Name of aliasTarget.
      */
    def cleanupIndexes(indexName: String = searchIndex): Try[String] = {
      e4sClient.execute(getAliases()) match {
        case Success(s) =>
          val indexes = s.result.mappings.filter(_._1.name.startsWith(indexName))
          val unreferencedIndexes = indexes.filter(_._2.isEmpty).map(_._1.name).toList
          val (aliasTarget, aliasIndexesToDelete) = indexes.filter(_._2.nonEmpty).map(_._1.name) match {
            case head :: tail =>
              (head, tail)
            case _ =>
              logger.warn("No alias found, when attempting to clean up indexes.")
              ("", List.empty)
          }

          val toDelete = unreferencedIndexes ++ aliasIndexesToDelete

          if (toDelete.isEmpty) {
            logger.info("No indexes to be deleted.")
            Success(aliasTarget)
          } else {
            e4sClient.execute {
              deleteIndex(toDelete)
            } match {
              case Success(_) =>
                logger.info(s"Successfully deleted unreferenced and redundant indexes.")
                Success(aliasTarget)
              case Failure(ex) =>
                logger.error("Could not delete unreferenced and redundant indexes.")
                Failure(ex)
            }
          }
        case Failure(ex) =>
          logger.warn("Could not fetch aliases after updating alias.")
          Failure(ex)
      }

    }

    def deleteAlias(): Try[Option[String]] = {
      getAliasTarget match {
        case Failure(ex)   => Failure(ex)
        case Success(None) => Success(None)
        case Success(Some(toDelete)) =>
          e4sClient.execute(removeAlias(searchIndex, toDelete)) match {
            case Failure(ex) => Failure(ex)
            case Success(_)  => Success(Some(toDelete))
          }
      }
    }

    def deleteIndexAndAlias(): Try[_] = {
      for {
        indexToDelete <- deleteAlias()
        _ <- deleteIndexWithName(indexToDelete)
      } yield ()
    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) =>
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.execute {
              deleteIndex(indexName)
            }
          }
      }

    }

    def countDocuments: Long = {
      val response = e4sClient.execute {
        catCount(searchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_)                          => Success(false)
        case Failure(ex)                         => Failure(ex)
      }
    }

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)

    /**
      * Returns Sequence of FieldDefinitions for a given field.
      *
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(
        fieldName: String,
        keepRaw: Boolean = false
    ): Seq[ElasticField] = {
      Language.languageAnalyzers.map(langAnalyzer => {
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

    /**
      * Returns Sequence of DynamicTemplateRequest for a given field.
      *
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of DynamicTemplateRequest for a field.
      */
    protected def generateLanguageSupportedDynamicTemplates(fieldName: String,
                                                            keepRaw: Boolean = false): Seq[DynamicTemplateRequest] = {
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

      val languageTemplates = Language.languageAnalyzers.map(
        languageAnalyzer => {
          val name = s"$fieldName.${languageAnalyzer.languageTag.toString()}"
          dynamicFunc(name, languageAnalyzer.analyzer, subFields)
        }
      )
      val languageSubTemplates = Language.languageAnalyzers.map(
        languageAnalyzer => {
          val name = s"*.$fieldName.${languageAnalyzer.languageTag.toString()}"
          dynamicFunc(name, languageAnalyzer.analyzer, subFields)
        }
      )
      val catchAllTemplate = dynamicFunc(s"$fieldName.*", "standard", subFields)
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

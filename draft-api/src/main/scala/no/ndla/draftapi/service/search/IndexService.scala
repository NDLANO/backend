/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties.ElasticSearchIndexMaxResultWindow
import no.ndla.draftapi._
import no.ndla.draftapi.model.domain.{Content, ReindexResult}
import no.ndla.draftapi.repository.Repository
import no.ndla.search.SearchLanguage.languageAnalyzers
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient with BaseIndexService =>

  trait IndexService[D <: Content, T <: AnyRef] extends BaseIndexService with LazyLogging {
    override val MaxResultWindowOption: Int = ElasticSearchIndexMaxResultWindow
    val repository: Repository[D]

    def createIndexRequests(domainModel: D, indexName: String): Seq[IndexRequest]

    def indexDocument(imported: D): Try[D] = {
      for {
        _ <- createIndexIfNotExists()
        requests = createIndexRequests(imported, searchIndex)
        _ <- executeRequests(requests)
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndexWithGeneratedName.flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- getAliasTarget
            _ <- updateAliasTarget(aliasTarget, indexName)
            _ <- deleteIndexWithName(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              deleteIndexWithName(Some(indexName))
              Failure(f)
            }
            case Success(totalIndexed) => {
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
            }
          }
        })
      }
    }

    def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = indexDocuments(repository.documentsWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f)   => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges: Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = repository.minMaxId
        Seq
          .range(minId, maxId + 1)
          .grouped(DraftApiProperties.IndexBulkSize)
          .map(group => (group.head, group.last))
          .toList
      }
    }

    def indexDocuments(contents: Seq[D], indexName: String): Try[Int] = {
      if (contents.isEmpty) {
        Success(0)
      } else {
        val requests = contents.flatMap(content => {
          createIndexRequests(content, indexName)
        })

        executeRequests(requests) match {
          case Success((numSuccessful, numFailures)) =>
            logger.info(s"Indexed $numSuccessful documents ($searchIndex). No of failed items: $numFailures")
            Success(contents.size)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def findAllIndexes(indexName: String): Try[Seq[String]] = {
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

    /**
      * Executes elasticsearch requests in bulk.
      * Returns success (without executing anything) if supplied with an empty list.
      *
      * @param requests a list of elasticsearch [[IndexRequest]]'s
      * @return A Try suggesting if the request was successful or not with a tuple containing number of successful requests and number of failed requests (in that order)
      */
    private def executeRequests(requests: Seq[IndexRequest]): Try[(Long, Long)] = {
      requests match {
        case Nil         => Success((0, 0))
        case head :: Nil => e4sClient.execute(head).map(r => if (r.isSuccess) (1, 0) else (0, 1))
        case reqs        => e4sClient.execute(bulk(reqs)).map(r => (r.result.successes.size, r.result.failures.size))
      }
    }

    /**
      * Returns Sequence of FieldDefinitions for a given field.
      *
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(fieldName: String, keepRaw: Boolean = false): Seq[ElasticField] = {
      keepRaw match {
        case true =>
          languageAnalyzers.map(
            langAnalyzer =>
              textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
                .fielddata(false)
                .analyzer(langAnalyzer.analyzer)
                .fields(keywordField("raw")))
        case false =>
          languageAnalyzers.map(
            langAnalyzer =>
              textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
                .fielddata(false)
                .analyzer(langAnalyzer.analyzer))
      }
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
      val fields = new ListBuffer[ElasticField]()
      if (keepRaw) {
        fields += keywordField("raw")
      }
      val languageTemplates = languageAnalyzers.map(
        languageAnalyzer => {
          val name = s"$fieldName.${languageAnalyzer.languageTag.toString()}"
          DynamicTemplateRequest(
            name = name,
            mapping = textField(name).analyzer(languageAnalyzer.analyzer).fields(fields.toList),
            matchMappingType = Some("string"),
            pathMatch = Some(name)
          )
        }
      )
      val catchAlltemplate = DynamicTemplateRequest(
        name = fieldName,
        mapping = textField(fieldName).analyzer(SearchLanguage.standardAnalyzer).fields(fields.toList),
        matchMappingType = Some("string"),
        pathMatch = Some(s"$fieldName.*")
      )
      languageTemplates ++ Seq(catchAlltemplate)
    }

  }
}

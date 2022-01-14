/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.domain.ReindexResult
import no.ndla.audioapi.repository.{AudioRepository, Repository}
import no.ndla.search.{Elastic4sClient, SearchLanguage}
import no.ndla.search.SearchLanguage.languageAnalyzers
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient with SearchConverterService with AudioRepository =>

  trait IndexService[D, T] extends LazyLogging {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val documentType: String
    val searchIndex: String
    val repository: Repository[D]

    def getMapping: MappingDefinition
    def createIndexRequests(domainModel: D, indexName: String): Try[Seq[IndexRequest]]

    private def createIndexIfNotExists() = getAliasTarget.map {
      case Some(index) => Success(index)
      case None        => createIndexWithGeneratedName.flatMap(newIndex => updateAliasTarget(None, newIndex))
    }

    def indexDocument(imported: D): Try[D] = {
      for {
        _ <- createIndexIfNotExists()
        requests <- createIndexRequests(imported, searchIndex)
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
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val documentsToIndex = repository.documentsWithIdBetween(range._1, range._2)
          val numberInBulk = documentsToIndex.flatMap(indexDocuments(_, indexName))
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f)   => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges: Try[List[(Long, Long)]] = {
      val minMaxT = repository.minMaxId
      minMaxT.flatMap {
        case (minId, maxId) =>
          Try {
            Seq
              .range(minId, maxId + 1)
              .grouped(AudioApiProperties.IndexBulkSize)
              .map(group => (group.head, group.last))
              .toList
          }
      }

    }

    def indexDocuments(contents: Seq[D], indexName: String): Try[Int] = {
      if (contents.isEmpty) {
        Success(0)
      } else {
        val requests = contents.traverse(content => createIndexRequests(content, indexName))
        requests.flatMap(rs => {
          executeRequests(rs.flatten) match {
            case Success((numSuccessful, numFailures)) =>
              logger.info(s"Indexed $numSuccessful documents ($searchIndex). No of failed items: $numFailures")
              Success(contents.size)
            case Failure(ex) => Failure(ex)
          }
        })

      }
    }

    def deleteDocument(contentId: Long): Try[Long] = {
      for {
        _ <- createIndexIfNotExists()
        _ <- {
          e4sClient.execute(
            deleteById(searchIndex, s"$contentId")
          )
        }
      } yield contentId
    }

    def createIndexWithGeneratedName: Try[String] = createIndexWithName(searchIndex + "_" + getTimestamp)

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          createIndex(indexName)
            .mapping(getMapping)
            .indexSetting("max_result_window", AudioApiProperties.ElasticSearchIndexMaxResultWindow)
        }

        response match {
          case Success(_)  => Success(indexName)
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

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        oldIndexName match {
          case None => e4sClient.execute(addAlias(searchIndex, newIndexName))
          case Some(oldIndex) =>
            e4sClient.execute {
              removeAlias(searchIndex, oldIndex)
              addAlias(searchIndex, newIndexName)
            }
        }
      }
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

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)

    /**
      * @deprecated Returns Sequence of FieldDefinitions for a given field.
      *
      * @param fieldName Name of field in mapping.
      * @param keepRaw   Whether to add a keywordField named raw.
      *                  Usually used for sorting, aggregations or scripts.
      * @return Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(fieldName: String, keepRaw: Boolean = false): Seq[ElasticField] = {
      if (keepRaw) {
        languageAnalyzers.map(
          langAnalyzer =>
            textField(s"$fieldName.${langAnalyzer.languageTag.toString()}")
              .analyzer(langAnalyzer.analyzer)
              .fields(keywordField("raw")))
      } else {
        languageAnalyzers.map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString()}").analyzer(langAnalyzer.analyzer))
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

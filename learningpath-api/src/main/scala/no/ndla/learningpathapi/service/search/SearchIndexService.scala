/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.RequestSuccess
import com.sksamuel.elastic4s.fields.{ElasticField, ObjectField}
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.SearchApiClient
import no.ndla.learningpathapi.model.domain.{ElasticIndexingException, ReindexResult}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.search.SearchLanguage.languageAnalyzers
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import cats.implicits.*
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.network.tapir.auth.TokenUser

trait SearchIndexService {
  this: Elastic4sClient
    with SearchConverterServiceComponent
    with LearningPathRepositoryComponent
    with SearchApiClient
    with BaseIndexService
    with Props =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends BaseIndexService with StrictLogging {
    import props.{SearchDocument, SearchIndex, ElasticSearchIndexMaxResultWindow}
    override val documentType: String       = SearchDocument
    override val searchIndex: String        = SearchIndex
    override val MaxResultWindowOption: Int = ElasticSearchIndexMaxResultWindow

    def indexDocuments: Try[ReindexResult] = indexDocuments(None)
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
            case Success(totalIndexed) => Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
          }
        })
      }
    }

    def indexDocument(learningPath: LearningPath): Try[LearningPath] = for {
      _ <- createIndexIfNotExists()
      searchable = searchConverterService.asSearchableLearningpath(learningPath)
      source     = CirceUtil.toJsonString(searchable)
      _ <- e4sClient.execute(deleteById(searchIndex, learningPath.id.get.toString))
      _ <- e4sClient.execute(
        indexInto(searchIndex)
          .doc(source)
          .id(learningPath.id.get.toString)
      )
    } yield learningPath

    def deleteDocument(learningPath: LearningPath, user: Option[TokenUser]): Try[LearningPath] = {
      learningPath.id
        .map(id => {
          for {
            _ <- createIndexIfNotExists()
            _ <- {
              e4sClient.execute {
                deleteById(searchIndex, id.toString)
              }
            }
            _ <- searchApiClient.deleteLearningPathDocument(id, user)
          } yield learningPath
        })
        .getOrElse(Success(learningPath))
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

    private def sendToElastic(indexName: String): Try[Int] = {
      getRanges
        .flatMap(ranges => {
          ranges.traverse { case (start, end) =>
            val toIndex = learningPathRepository.learningPathsWithIdBetween(start, end)
            indexLearningPaths(toIndex, indexName)
          }
        })
        .map(_.sum)
    }

    private def getRanges: Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = learningPathRepository.minMaxId
        Seq
          .range(minId, maxId)
          .grouped(props.IndexBulkSize)
          .map(group => (group.head, group.last + 1))
          .toList
      }
    }

    private def indexLearningPaths(learningPaths: List[LearningPath], indexName: String): Try[Int] = {
      if (learningPaths.isEmpty) {
        Success(0)
      } else {
        val searchables = learningPaths.map(searchConverterService.asSearchableLearningpath)
        val requests = searchables.map(lp => {
          val source = CirceUtil.toJsonString(lp)

          indexInto(indexName)
            .doc(source)
            .id(lp.id.toString)
        })

        val response = e4sClient.execute(bulk(requests))
        response match {
          case Success(RequestSuccess(_, _, _, result)) if !result.errors =>
            logger.info(s"Indexed ${learningPaths.size} documents")
            Success(learningPaths.size)
          case Success(RequestSuccess(_, _, _, result)) =>
            val failed = result.items.collect {
              case item if item.error.isDefined =>
                s"'${item.id}: ${item.error.get.reason}'"
            }

            logger.error(s"Failed to index ${failed.length} items: ${failed.mkString(", ")}")
            Failure(ElasticIndexingException(s"Failed to index ${failed.size}/${learningPaths.size} learningpaths"))
          case Failure(ex) => Failure(ex)
        }
      }
    }

    override def getMapping: MappingDefinition = {
      val fields = List(
        intField("id"),
        textField("coverPhotoUrl"),
        intField("duration"),
        keywordField("status"),
        keywordField("verificationStatus"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        textField("author"),
        nestedField("learningsteps").fields(
          textField("stepType"),
          keywordField("embedUrl"),
          keywordField("status")
        ),
        ObjectField(
          "copyright",
          properties = Seq(
            ObjectField(
              "license",
              properties = Seq(
                textField("license"),
                textField("description"),
                textField("url")
              )
            ),
            nestedField("contributors").fields(
              textField("type"),
              textField("name")
            )
          )
        ),
        intField("isBasedOn")
      )
      val dynamics = generateLanguageSupportedDynamicTemplates("titles", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("descriptions") ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = true)

      properties(fields).dynamicTemplates(dynamics)
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
      val fields = new ListBuffer[ElasticField]()
      if (keepRaw) {
        fields += keywordField("raw")
      }
      val languageTemplates = languageAnalyzers.map(languageAnalyzer => {
        val name = s"$fieldName.${languageAnalyzer.languageTag.toString()}"
        dynamicFunc(name, languageAnalyzer.analyzer, fields.toList)
      })
      val languageSubTemplates = languageAnalyzers.map(languageAnalyzer => {
        val name = s"*.$fieldName.${languageAnalyzer.languageTag.toString()}"
        dynamicFunc(name, languageAnalyzer.analyzer, fields.toList)
      })
      val catchAllTemplate = dynamicFunc(s"$fieldName.*", SearchLanguage.standardAnalyzer, fields.toList)
      languageTemplates ++ languageSubTemplates ++ Seq(catchAllTemplate)
    }

  }

}

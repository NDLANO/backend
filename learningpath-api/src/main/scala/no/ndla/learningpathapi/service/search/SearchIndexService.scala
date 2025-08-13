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
import com.typesafe.scalalogging.StrictLogging
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.SearchApiClient
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

import scala.util.{Failure, Success, Try}
import cats.implicits.*
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.search.model.domain.{BulkIndexResult, ElasticIndexingException, ReindexResult}

trait SearchIndexService {
  this: Elastic4sClient & SearchConverterServiceComponent & LearningPathRepositoryComponent & SearchApiClient &
    BaseIndexService & Props & SearchLanguage =>
  lazy val searchIndexService: SearchIndexService

  class SearchIndexService extends BaseIndexService with StrictLogging {
    override val documentType: String       = props.SearchDocument
    override val searchIndex: String        = props.SearchIndex
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

    def indexDocuments: Try[ReindexResult]                         = indexDocuments(None)
    def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = synchronized {
      indexDocumentsInBulk(numShards)(sendToElastic)
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

    private def sendToElastic(indexName: String): Try[BulkIndexResult] = {
      getRanges
        .flatMap(ranges => {
          ranges
            .traverse { case (start, end) =>
              val toIndex = learningPathRepository.learningPathsWithIdBetween(start, end)
              indexLearningPaths(toIndex, indexName).map(numIndexed => (numIndexed, toIndex.size))
            }
            .map(countIndexed)
        })
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
        val requests    = searchables.map(lp => {
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
        dateField("created"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        textField("author"),
        nestedField("learningsteps").fields(
          textField("stepType"),
          keywordField("embedUrl"),
          keywordField("status"),
          intField("articleId")
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
      val dynamics = generateLanguageSupportedFieldList("titles", keepRaw = true) ++
        generateLanguageSupportedFieldList("descriptions") ++
        generateLanguageSupportedFieldList("tags", keepRaw = true)

      properties(fields ++ dynamics)
    }

    protected def generateLanguageSupportedFieldList(fieldName: String, keepRaw: Boolean = false): Seq[ElasticField] = {
      if (keepRaw) {
        SearchLanguage.languageAnalyzers.map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
            .analyzer(langAnalyzer.analyzer)
            .fields(
              keywordField("raw")
            )
        )
      } else {
        SearchLanguage.languageAnalyzers.map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
            .analyzer(langAnalyzer.analyzer)
        )
      }
    }
  }

}

/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.alias.AliasAction
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.search.SearchLanguage.NynorskLanguageAnalyzer

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.util.{Failure, Success, Try}

trait BaseIndexService {
  this: Elastic4sClient =>

  trait BaseIndexService extends LazyLogging {
    val documentType: String
    val searchIndex: String
    val MaxResultWindowOption: Int

    val analysis: Analysis =
      Analysis(
        analyzers = List(NynorskLanguageAnalyzer),
        tokenFilters = SearchLanguage.NynorskTokenFilters
      )

    def getMapping: MappingDefinition

    // Setting this to suppress the warning, since it will default to '1' in the new version.
    // The value '5' was chosen since that is what was the default earlier (which worked fine).
    // However there are probably more optimal values.
    val indexShards: Int = 5

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

    protected def buildCreateIndexRequest(indexName: String): CreateIndexRequest = {
      createIndex(indexName)
        .shards(indexShards)
        .mapping(getMapping)
        .indexSetting("max_result_window", MaxResultWindowOption)
        .replicas(0)
        .analysis(analysis)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          buildCreateIndexRequest(indexName)
        }

        response match {
          case Success(_)  => Success(indexName)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def deleteDocument(contentId: Long): Try[Long] = {
      for {
        _ <- createIndexIfNotExists()
        deleted <- {
          e4sClient.execute {
            deleteById(searchIndex, s"$contentId")
          }
        }
      } yield contentId
    }

    def createIndexWithGeneratedName: Try[String] = createIndexWithName(searchIndex + "_" + getTimestamp)

    def createIndexIfNotExists(): Try[_] = getAliasTarget.flatMap {
      case Some(index) => Success(index)
      case None        => createIndexAndAlias()
    }

    def createIndexAndAlias(): Try[String] = {
      createIndexWithGeneratedName.map(newIndex => {
        updateAliasTarget(None, newIndex)
        newIndex
      })
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

    /** Deletes every index that is not in use by this indexService. Only indexes starting with indexName are deleted.
      *
      * @param indexName
      *   Start of index names that is deleted if not aliased.
      * @return
      *   Name of aliasTarget.
      */
    def cleanupIndexes(indexName: String = searchIndex): Try[String] = {
      e4sClient.execute(getAliases()) match {
        case Success(s) =>
          val indexes             = s.result.mappings.filter(_._1.name.startsWith(indexName))
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
        _             <- deleteIndexWithName(indexToDelete)
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

    def getTimestamp: String = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
  }
}

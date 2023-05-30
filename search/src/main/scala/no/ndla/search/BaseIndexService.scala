/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.alias.AliasAction
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.search.SearchLanguage.NynorskLanguageAnalyzer

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.util.{Failure, Success, Try}

trait BaseIndexService {
  this: Elastic4sClient with HasBaseProps =>

  trait BaseIndexService extends StrictLogging {
    val documentType: String
    val searchIndex: String
    val MaxResultWindowOption: Int

    val analysis: Analysis =
      Analysis(
        analyzers = List(NynorskLanguageAnalyzer),
        tokenFilters = SearchLanguage.NynorskTokenFilters
      )

    def getMapping: MappingDefinition

    val indexShards: Int   = props.SEARCH_INDEX_SHARDS
    val indexReplicas: Int = props.SEARCH_INDEX_REPLICAS

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

    protected def buildCreateIndexRequest(indexName: String, numShards: Option[Int]): CreateIndexRequest = {
      createIndex(indexName)
        .shards(numShards.getOrElse(indexShards))
        .mapping(getMapping)
        .indexSetting("max_result_window", MaxResultWindowOption)
        .replicas(0) // Spawn with 0 replicas to make indexing faster
        .analysis(analysis)
    }
    def createIndexWithName(indexName: String): Try[String] = createIndexWithName(indexName, None)

    def createIndexWithName(indexName: String, numShards: Option[Int]): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          buildCreateIndexRequest(indexName, numShards)
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

    def getNewIndexName() = s"${searchIndex}_$getTimestamp"

    def createIndexWithGeneratedName(numShards: Option[Int]): Try[String] =
      createIndexWithName(getNewIndexName(), numShards)

    def createIndexWithGeneratedName: Try[String] =
      createIndexWithName(getNewIndexName())

    def reindexWithShards(numShards: Int): Try[_] = {
      logger.info(s"Internal reindexing $searchIndex with $numShards shards...")
      val maybeAliasTarget = getAliasTarget.?
      val currentIndex = maybeAliasTarget match {
        case Some(target) => target
        case None =>
          logger.info(s"No existing $searchIndex index to reindex from")
          return Success(())
      }

      for {
        newIndex <- createIndexWithGeneratedName(numShards.some)
        _ = logger.info(s"Created index $newIndex for internal reindexing")
        _ <- e4sClient.execute(reindex(currentIndex, newIndex))
        _ <- updateAliasTarget(currentIndex.some, newIndex)
      } yield ()
    }

    def createIndexIfNotExists(): Try[_] = getAliasTarget.flatMap {
      case Some(index) => Success(index)
      case None        => createIndexAndAlias(indexShards.some)
    }

    def createIndexAndAlias(): Try[String] = createIndexAndAlias(None)
    def createIndexAndAlias(numberOfShards: Option[Int]): Try[String] = {
      createIndexWithGeneratedName(numberOfShards).map(newIndex => {
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

    def updateReplicaNumber(overrideReplicaNumber: Int): Try[_] = getAliasTarget.flatMap {
      case None => Success(())
      case Some(indexName) =>
        updateReplicaNumber(indexName, overrideReplicaNumber.some)
    }

    private def updateReplicaNumber(indexName: String, overrideReplicaNumber: Option[Int]): Try[_] = {
      if (props.Environment == "local") {
        logger.info("Skipping replica change in local environment, since the cluster only has one node.")
        Success(())
      } else {
        val settingsMap = Map("number_of_replicas" -> overrideReplicaNumber.getOrElse(indexReplicas).toString)
        e4sClient.execute(updateSettings(Indexes(indexName), settingsMap))
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
              _ <- updateReplicaNumber(newIndexName, overrideReplicaNumber = None)
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

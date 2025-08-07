/*
 * Part of NDLA search
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.HasBaseProps

trait BaseIndexService {
  this: Elastic4sClient & HasBaseProps & SearchLanguage =>

  trait BaseIndexService {

    /** Replace index even if bulk indexing had failures */
    // def createIndexWithName(indexName: String): Try[String] = ???
    // def createIndexWithName(indexName: String, numShards: Option[Int]): Try[String]                = ???
    // def deleteDocument(contentId: Long): Try[Long]                                                 = ???
    def getNewIndexName(): String = ???
    // def createIndexWithGeneratedName(numShards: Option[Int]): Try[String]                          = ???
    // protected def validateBulkIndexing(indexResult: BulkIndexResult): Try[BulkIndexResult]         = ???
    // def countBulkIndexed(indexChunks: List[BulkIndexResult]): BulkIndexResult                      = ???
    // def countIndexed(indexChunks: List[(Int, Int)]): BulkIndexResult                               = ???
    // def createIndexWithGeneratedName: Try[String]                                                  = ???
    // def reindexWithShards(numShards: Int): Try[?]                                                  = ???
    // def createIndexIfNotExists(): Try[?]                                                           = ???
    // def createIndexAndAlias(): Try[String]                                                         = ???
    // def createIndexAndAlias(numberOfShards: Option[Int]): Try[String]                              = ???
    // def getAliasTarget: Try[Option[String]]                                                        = ???
    // def updateReplicaNumber(overrideReplicaNumber: Int): Try[?]                                    = ???
    // private def updateReplicaNumber(indexName: String, overrideReplicaNumber: Option[Int]): Try[?] = ???
    // def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any]            = ???
    // def deleteIndexWithName(optIndexName: Option[String]): Try[?]                                  = ???
    // def countDocuments: Long                                                                       = ???
    // def getTimestamp: String                                                                       = ???
    // def findAllIndexes(indexName: String): Try[Seq[String]]                                        = ???

  }
}

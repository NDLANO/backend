/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.myndla.model.domain.MyNDLAUserDocument
import no.ndla.myndla.repository.{ConfigRepository, FolderRepository, FolderRow, UserRepository}
import scalikejdbc.{ConnectionPool, DBSession, DataSourceConnectionPool, NamedDB}

import scala.annotation.tailrec

// TODO: Delete this when migration is done
case class LpMigration(props: MyNdlaApiProperties, localDataSource: HikariDataSource, lpDataSource: HikariDataSource)
    extends FolderRepository
    with UserRepository
    with ConfigRepository
    with Clock
    with StrictLogging {
  override val folderRepository: FolderRepository = new FolderRepository
  override val userRepository: UserRepository     = new UserRepository
  override val configRepository: ConfigRepository = new ConfigRepository
  override val clock: SystemClock                 = new SystemClock

  private def migrateConfig(lpSession: DBSession, myndlaSession: DBSession): Unit = {
    val allLpConfigs = configRepository.getAllConfigs(lpSession)
    allLpConfigs.foreach(x => configRepository.updateConfigParam(x)(myndlaSession))
  }

  private def migrateUsers(lpSession: DBSession, myndlaSession: DBSession): Unit = {
    val oldUsers = userRepository.getAllUsers(lpSession)
    oldUsers.foreach(x => {
      userRepository.reserveFeideIdIfNotExists(x.feideId)(myndlaSession): Unit
      userRepository.insertUser(
        x.feideId,
        MyNDLAUserDocument(
          favoriteSubjects = x.favoriteSubjects,
          userRole = x.userRole,
          lastUpdated = x.lastUpdated,
          organization = x.organization,
          groups = x.groups,
          username = x.username,
          displayName = x.displayName,
          email = x.email,
          arenaEnabled = x.arenaEnabled,
          shareName = x.shareName,
          arenaGroups = List.empty
        )
      )(myndlaSession)
    })
  }

  private def migrateFolders(lpSession: DBSession, myndlaSession: DBSession): Unit = {
    val folders         = folderRepository.getAllFolderRows(lpSession)
    val resources       = folderRepository.getAllResourceRows(lpSession)
    val folderResources = folderRepository.getAllFolderResourceRows(lpSession)

    // To make sure the parent folder exists before inserting the child folder
    @tailrec
    def insertFolderRecursivly(inserted: List[FolderRow], toInsert: List[FolderRow]): Unit = {
      if (toInsert.isEmpty) return

      val (parentExists, parentDoesntExist) =
        toInsert.partition(x => x.parent_id.isEmpty || inserted.exists(i => x.parent_id.contains(i.id)))
      parentExists.foreach(x => folderRepository.insertFolderRow(x)(myndlaSession))
      insertFolderRecursivly(inserted ++ parentExists, parentDoesntExist)
    }

    insertFolderRecursivly(List.empty[FolderRow], folders)
    resources.foreach(x => folderRepository.insertResourceRow(x)(myndlaSession))
    folderResources.foreach(x => folderRepository.insertFolderResourceRow(x)(myndlaSession))
  }

  private def detectExistingMigration(myndlaSession: DBSession): Boolean = {
    configRepository.getAllConfigs(myndlaSession).nonEmpty

  }

  def start(): Unit = {
    val lpPool     = new DataSourceConnectionPool(lpDataSource)
    val myNdlaPool = new DataSourceConnectionPool(localDataSource)
    ConnectionPool.add(Symbol("learningpath"), lpPool)
    ConnectionPool.add(Symbol("myndla"), myNdlaPool)

    NamedDB(Symbol("learningpath")).readOnly { lpSession =>
      NamedDB(Symbol("myndla")).localTx { myndlaSession =>
        if (!detectExistingMigration(myndlaSession)) {
          logger.info("Doing learningpath-api -> myndla-api migration!")

          migrateConfig(lpSession, myndlaSession)
          migrateUsers(lpSession, myndlaSession)
          migrateFolders(lpSession, myndlaSession)
        } else {
          logger.info("Migration already done! Please delete `LpMigration` from `myndla-api`.")
        }
      }
    }
  }
}

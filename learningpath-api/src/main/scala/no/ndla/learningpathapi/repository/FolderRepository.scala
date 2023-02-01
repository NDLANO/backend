/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.domain._
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}
import cats.implicits._
import no.ndla.common.Clock
import no.ndla.common.errors.RollbackException

trait FolderRepository {
  this: DataSource with DBFolder with DBResource with DBFolderResource with Clock =>
  val folderRepository: FolderRepository

  class FolderRepository extends StrictLogging {
    implicit val formats: Formats = DBFolder.repositorySerializer + DBResource.JSonSerializer

    def getSession(readOnly: Boolean): DBSession =
      if (readOnly) ReadOnlyAutoSession
      else AutoSession

    def withTx[T](func: DBSession => T): T =
      DB.localTx { session => func(session) }

    def rollbackOnFailure[T](func: DBSession => Try[T]): Try[T] = {
      try {
        DB.localTx { session =>
          func(session) match {
            case Failure(ex)    => throw RollbackException(ex)
            case Success(value) => Success(value)
          }
        }
      } catch {
        case RollbackException(ex) => Failure(ex)
      }
    }

    def insertFolder(
        feideId: FeideID,
        folderData: NewFolderData
    )(implicit session: DBSession = AutoSession): Try[Folder] =
      Try {
        val newId   = UUID.randomUUID()
        val created = clock.now()
        val updated = created
        val shared  = if (folderData.status == FolderStatus.SHARED) Some(clock.now()) else None

        sql"""
        insert into ${DBFolder.table} (id, parent_id, feide_id, name, status, rank, created, updated, shared)
        values ($newId, ${folderData.parentId}, $feideId, ${folderData.name}, ${folderData.status.toString}, ${folderData.rank}, $created, $updated, $shared)
        """.update()

        logger.info(s"Inserted new folder with id: $newId")
        folderData.toFullFolder(
          id = newId,
          feideId = feideId,
          resources = List.empty,
          subfolders = List.empty,
          created = created,
          updated = updated,
          shared = shared
        )
      }

    def insertResource(
        feideId: FeideID,
        path: String,
        resourceType: String,
        created: LocalDateTime,
        document: ResourceDocument
    )(implicit session: DBSession = AutoSession): Try[Resource] = Try {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(document))

      val newId = UUID.randomUUID()

      sql"""
        insert into ${DBResource.table} (id, feide_id, path, resource_type, created, document)
        values ($newId, $feideId, $path, $resourceType, $created, $dataObject)
        """.update()

      logger.info(s"Inserted new resource with id: $newId")
      document.toFullResource(newId, path, resourceType, feideId, created, None)
    }

    def createFolderResourceConnection(
        folderId: UUID,
        resourceId: UUID,
        rank: Int
    )(implicit session: DBSession = AutoSession): Try[FolderResource] = Try {
      sql"insert into ${DBFolderResource.table} (folder_id, resource_id, rank) values ($folderId, $resourceId, $rank)"
        .update()
      logger.info(s"Inserted new folder-resource connection with folder id $folderId and resource id $resourceId")

      FolderResource(folderId = folderId, resourceId = resourceId, rank = rank)
    }

    def updateFolder(id: UUID, feideId: FeideID, folder: Folder)(implicit
        session: DBSession = AutoSession
    ): Try[Folder] = Try {
      sql"""
          update ${DBFolder.table}
          set name=${folder.name},
              status=${folder.status.toString},
              shared=${folder.shared}
          where id=$id and feide_id=$feideId
      """.update()
    } match {
      case Failure(ex) => Failure(ex)
      case Success(count) if count == 1 =>
        logger.info(s"Updated folder with id $id")
        Success(folder)
      case Success(count) =>
        Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
    }

    def updateFolderStatusInBulk(folderIds: List[UUID], newStatus: FolderStatus.Value)(implicit
        session: DBSession = AutoSession
    ): Try[List[UUID]] = Try {
      sql"""
             UPDATE ${DBFolder.table}
             SET status = ${newStatus.toString}
             where id in ($folderIds);
           """.update()
    } match {
      case Failure(ex) => Failure(ex)
      case Success(count) if count == folderIds.length =>
        logger.info(s"Updated folders with ids (${folderIds.mkString(", ")})")
        Success(folderIds)
      case Success(count) =>
        Failure(
          NDLASQLException(s"This is a Bug! The expected rows count should be ${folderIds.length} and was $count.")
        )
    }

    def updateResource(resource: Resource)(implicit session: DBSession = AutoSession): Try[Resource] = Try {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(resource))

      sql"""
          update ${DBResource.table}
          set document=$dataObject
          where id=${resource.id}
      """.update()
    } match {
      case Failure(ex) => Failure(ex)
      case Success(count) if count == 1 =>
        logger.info(s"Updated resource with id ${resource.id}")
        Success(resource)
      case Success(count) =>
        Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
    }

    def folderResourceConnectionCount(resourceId: UUID)(implicit session: DBSession = AutoSession): Try[Long] = {
      Try(
        sql"select count(*) from ${DBFolderResource.table} where resource_id=$resourceId"
          .map(rs => rs.long("count"))
          .single()
          .getOrElse(0)
      )
    }

    def getConnection(folderId: UUID, resourceId: UUID)(implicit
        session: DBSession = AutoSession
    ): Try[Option[FolderResource]] = {
      Try(
        sql"select resource_id, folder_id, rank from ${DBFolderResource.table} where resource_id=$resourceId and folder_id=$folderId"
          .map(rs => {
            for {
              resourceId <- rs.get[Try[UUID]]("resource_id")
              folderId   <- rs.get[Try[UUID]]("folder_id")
              rank = rs.int("rank")
            } yield FolderResource(resourceId, folderId, rank)
          })
          .single()
      )
        .map(_.sequence)
        .flatten
    }

    def getConnections(folderId: UUID)(implicit session: DBSession = AutoSession): Try[List[FolderResource]] = {
      Try(
        sql"select resource_id, folder_id, rank from ${DBFolderResource.table} where folder_id=$folderId"
          .map(rs => {
            for {
              resourceId <- rs.get[Try[UUID]]("resource_id")
              folderId   <- rs.get[Try[UUID]]("folder_id")
              rank = rs.int("rank")
            } yield FolderResource(folderId, resourceId, rank)
          })
          .list()
      )
        .map(_.sequence)
        .flatten
    }

    def deleteFolder(id: UUID)(implicit session: DBSession = AutoSession): Try[UUID] = {
      Try(sql"delete from ${DBFolder.table} where id = $id".update()) match {
        case Failure(ex)                      => Failure(ex)
        case Success(numRows) if numRows != 1 => Failure(NotFoundException(s"Folder with id $id does not exist"))
        case Success(_) =>
          logger.info(s"Deleted folder with id $id")
          Success(id)
      }
    }

    def deleteResource(id: UUID)(implicit session: DBSession = AutoSession): Try[UUID] = {
      Try(sql"delete from ${DBResource.table} where id = $id".update()) match {
        case Failure(ex)                      => Failure(ex)
        case Success(numRows) if numRows != 1 => Failure(NotFoundException(s"Resource with id $id does not exist"))
        case Success(_) =>
          logger.info(s"Deleted resource with id $id")
          Success(id)
      }
    }

    def deleteFolderResourceConnection(folderId: UUID, resourceId: UUID)(implicit
        session: DBSession = AutoSession
    ): Try[UUID] =
      Try(
        sql"delete from ${DBFolderResource.table} where folder_id=$folderId and resource_id=$resourceId".update()
      ) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) if numRows != 1 =>
          Failure(
            NotFoundException(
              s"Folder-Resource connection with folder_id $folderId and resource_id $resourceId does not exist"
            )
          )
        case Success(_) =>
          logger.info(s"Deleted folder-resource connection with folder_id $folderId and resource_id $resourceId")
          Success(resourceId)
      }

    def deleteAllUserFolders(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[Long] = {
      Try(sql"delete from ${DBFolder.table} where feide_id = $feideId".update()) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) =>
          logger.info(s"Deleted $numRows folders with feide_id = $feideId")
          Success(numRows)
      }
    }

    def deleteAllUserResources(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[Long] = {
      Try(sql"delete from ${DBResource.table} where feide_id = $feideId".update()) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) =>
          logger.info(s"Deleted $numRows resources with feide_id = $feideId")
          Success(numRows)
      }
    }

    def folderWithId(id: UUID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Folder] =
      folderWhere(sqls"f.id=$id").flatMap {
        case None         => Failure(NotFoundException(s"Folder with id $id does not exist"))
        case Some(folder) => Success(folder)
      }

    def folderWithFeideId(id: UUID, feideId: FeideID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Folder] =
      folderWhere(sqls"f.id=$id and f.feide_id=$feideId").flatMap {
        case None         => Failure(NotFoundException(s"Folder with id $id does not exist"))
        case Some(folder) => Success(folder)
      }

    def resourceWithId(id: UUID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Resource] =
      resourceWhere(sqls"r.id=$id").flatMap({
        case None           => Failure(NotFoundException(s"Resource with id $id does not exist"))
        case Some(resource) => Success(resource)
      })

    def resourcesWithFeideId(feideId: FeideID, size: Int)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Try[List[Resource]] =
      resourcesWhere(sqls"r.feide_id=$feideId order by r.created desc limit $size")

    def resourceWithPathAndTypeAndFeideId(
        path: String,
        resourceType: String,
        feideId: FeideID
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[Resource]] = resourceWhere(
      sqls"path=$path and resource_type=$resourceType and feide_id=$feideId"
    )

    def foldersWithFeideAndParentID(parentId: Option[UUID], feideId: FeideID)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Try[List[Folder]] = {
      val parentIdClause = parentId match {
        case Some(pid) => sqls"f.parent_id=$pid"
        case None      => sqls"f.parent_id is null"
      }
      foldersWhere(sqls"$parentIdClause and f.feide_id=$feideId")
    }

    private[repository] def buildTreeStructureFromListOfChildren(
        baseParentId: UUID,
        folders: List[Folder]
    ): Option[Folder] =
      folders match {
        case Nil => None
        case allTheStuffs =>
          allTheStuffs.find(_.id == baseParentId) match {
            case None => None
            case Some(mainParent) =>
              val byPid = allTheStuffs.groupBy(_.parentId)
              def injectChildrenRecursively(current: Folder): Folder = byPid.get(current.id.some) match {
                case Some(children) =>
                  val childrenWithTheirChildrenFolders =
                    children
                      .sortBy(_.id.toString)
                      .map(child => injectChildrenRecursively(child))

                  current.copy(subfolders = childrenWithTheirChildrenFolders)
                case None => current
              }
              injectChildrenRecursively(mainParent).some
          }
      }

    def getFolderAndChildrenSubfoldersWithResources(id: UUID)(implicit session: DBSession): Try[Option[Folder]] = {
      getFolderAndChildrenSubfoldersWithResourcesWhere(id, sqls"")
    }

    def getFolderAndChildrenSubfoldersWithResources(id: UUID, status: FolderStatus.Value)(implicit
        session: DBSession
    ): Try[Option[Folder]] = {
      getFolderAndChildrenSubfoldersWithResourcesWhere(id, sqls"and child.status = ${status.toString}")
    }

    /** A flat list of the folder with `id` as well as its children folders. The folders in the list comes with
      * connected resources in the `data` list.
      */
    private[repository] def getFolderAndChildrenSubfoldersWithResourcesWhere(id: UUID, sqlFilterClause: SQLSyntax)(
        implicit session: DBSession
    ): Try[Option[Folder]] = Try {
      sql"""-- Big recursive block which fetches the folder with `id` and also its children recursively
            WITH RECURSIVE childs AS (
                SELECT id AS f_id, parent_id AS f_parent_id, feide_id AS f_feide_id, name as f_name, status as f_status, rank AS f_rank, created as f_created, updated as f_updated, shared as f_shared
                FROM ${DBFolder.table} parent
                WHERE id = $id
                UNION ALL
                SELECT child.id AS f_id, child.parent_id AS f_parent_id, child.feide_id AS f_feide_id, child.name AS f_name, child.status as f_status, child.rank AS f_rank, child.created as f_created, child.updated as f_updated, child.shared as f_shared
                FROM ${DBFolder.table} child
                JOIN childs AS parent ON parent.f_id = child.parent_id
                $sqlFilterClause
            )
            SELECT * FROM childs
            LEFT JOIN folder_resources fr ON fr.folder_id = f_id
            LEFT JOIN ${DBResource.table} r ON r.id = fr.resource_id;
         """
        // We prefix the `folders` columns with `f_` to separate them
        // from the `folder_resources` columns  (both here and in sql).
        .one(rs => DBFolder.fromResultSet(s => s"f_$s")(rs))
        .toMany(rs => DBResource.fromResultSetOpt(rs, withConnection = true).sequence)
        .map((folder, resources) =>
          resources.toList.sequence.flatMap(resources => folder.map(f => f.copy(resources = resources)))
        )
        .list()
        .sequence
    }.flatten.map(data => buildTreeStructureFromListOfChildren(id, data))

    def getFolderAndChildrenSubfolders(id: UUID)(implicit session: DBSession): Try[Option[Folder]] = Try {
      sql"""-- Big recursive block which fetches the folder with `id` and also its children recursively
            WITH RECURSIVE childs AS (
                SELECT parent.*
                FROM ${DBFolder.table} parent
                WHERE id = $id
                UNION ALL
                SELECT child.*
                FROM ${DBFolder.table} child
                JOIN childs AS parent ON parent.id = child.parent_id
            )
            SELECT * FROM childs;
         """
        .map(rs => DBFolder.fromResultSet(rs))
        .list()
        .sequence
    }.flatten.map(data => buildTreeStructureFromListOfChildren(id, data))

    def getFoldersDepth(parentId: UUID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Long] = Try {
      sql"""
           WITH RECURSIVE parents AS (
                SELECT id AS f_id, parent_id AS f_parent_id, 0 dpth
                FROM ${DBFolder.table} child
                WHERE id = $parentId
                UNION ALL
                SELECT parent.id AS f_id, parent.parent_id AS f_parent_id, dpth +1
                FROM ${DBFolder.table} parent
                JOIN parents AS child ON child.f_parent_id = parent.id
            )
            SELECT * FROM parents ORDER BY parents.dpth DESC
         """
        .map(rs => rs.long("dpth"))
        .first()
        .getOrElse(0)
    }

    def getFoldersAndSubfoldersIds(folderId: UUID)(implicit session: DBSession = ReadOnlyAutoSession): Try[List[UUID]] =
      Try {
        sql"""
             WITH RECURSIVE parent (id) as (
                  SELECT id
                  FROM ${DBFolder.table} child
                  WHERE id = $folderId
                  UNION ALL
                  SELECT child.id
                  FROM ${DBFolder.table} child, parent
                  WHERE child.parent_id = parent.id
            )
            SELECT * FROM parent
           """
          .map(rs => rs.get[Try[UUID]]("id"))
          .list()
          .sequence
      }.flatten

    def foldersWithParentID(parentId: Option[UUID])(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Try[List[Folder]] = foldersWhere(sqls"f.parent_id=$parentId")

    def getFolderResources(
        folderId: UUID
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[Resource]] = Try {
      val fr = DBFolderResource.syntax("fr")
      val r  = DBResource.syntax("r")
      sql"""select ${r.result.*}, ${fr.result.*} from ${DBFolderResource.as(fr)}
            left join ${DBResource.as(r)}
                on ${fr.resourceId} = ${r.id}
            where ${fr.folderId} = $folderId;
           """
        .one(DBResource.fromResultSet(r, withConnection = false))
        .toOne(rs => DBFolderResource.fromResultSet(fr)(rs).toOption)
        .map((resource, connection) => resource.map(_.copy(connection = connection)))
        .list()
        .sequence
    }.flatten

    private def folderWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[Option[Folder]] = Try {
      val f = DBFolder.syntax("f")
      sql"select ${f.result.*} from ${DBFolder.as(f)} where $whereClause"
        .map(DBFolder.fromResultSet(f))
        .single()
        .sequence
    }.flatten

    private def foldersWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[List[Folder]] = Try {
      val f = DBFolder.syntax("f")
      sql"select ${f.result.*} from ${DBFolder.as(f)} where $whereClause"
        .map(DBFolder.fromResultSet(f))
        .list()
        .sequence
    }.flatten

    private def resourcesWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[List[Resource]] = Try {
      val r = DBResource.syntax("r")
      sql"select ${r.result.*} from ${DBResource.as(r)} where $whereClause"
        .map(DBResource.fromResultSet(r, withConnection = false))
        .list()
        .sequence
    }.flatten

    private def resourceWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[Option[Resource]] = Try {
      val r = DBResource.syntax("r")
      sql"select ${r.result.*} from ${DBResource.as(r)} where $whereClause"
        .map(DBResource.fromResultSet(r, withConnection = false))
        .single()
        .sequence
    }.flatten

    def setFolderRank(folderId: UUID, rank: Int, feideId: FeideID)(implicit session: DBSession): Try[Unit] = {
      Try {
        sql"""
          update ${DBFolder.table}
          set rank=$rank
          where id=$folderId and feide_id=$feideId
      """
          .update()
      } match {
        case Failure(ex) => Failure(ex)
        case Success(count) if count == 1 =>
          logger.info(s"Updated rank for folder with id $folderId")
          Success(())
        case Success(count) =>
          Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
      }
    }

    def setResourceConnectionRank(folderId: UUID, resourceId: UUID, rank: Int)(implicit session: DBSession): Try[Unit] =
      Try {
        sql"""
          update ${DBFolderResource.table}
          set rank=$rank
          where folder_id=$folderId and resource_id=$resourceId
      """.update()
      } match {
        case Failure(ex) => Failure(ex)
        case Success(count) if count == 1 =>
          logger.info(s"Updated rank for folder-resource connection with folderId $folderId and resourceId $resourceId")
          Success(())
        case Success(count) =>
          Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
      }

    def numberOfTags()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(tag) from (select distinct jsonb_array_elements_text(document->'tags') from ${DBResource.table}) as tag"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfResources()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${DBResource.table}"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfFolders()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${DBFolder.table}"
        .map(rs => rs.long("count"))
        .single()
    }

  }
}

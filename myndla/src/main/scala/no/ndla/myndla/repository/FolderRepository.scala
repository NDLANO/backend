/*
 * Part of NDLA myndla.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndla.repository

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.errors.{NotFoundException, RollbackException}
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain.{
  DBFolder,
  DBFolderResource,
  DBResource,
  Folder,
  FolderResource,
  FolderStatus,
  NDLASQLException,
  NewFolderData,
  Resource,
  ResourceDocument
}
import no.ndla.myndla.{uuidBinder, maybeUuidBinder, uuidParameterFactory}
import no.ndla.network.model.FeideID
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax

import java.util.UUID
import scala.util.{Failure, Success, Try}

trait FolderRepository {
  this: Clock =>
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
        val shared  = if (folderData.status == FolderStatus.SHARED) Some(created) else None

        val column = DBFolder.column.c _
        withSQL {
          insert
            .into(DBFolder)
            .namedValues(
              column("id")          -> newId,
              column("parent_id")   -> folderData.parentId,
              column("feide_id")    -> feideId,
              column("name")        -> folderData.name,
              column("status")      -> folderData.status.toString,
              column("rank")        -> folderData.rank,
              column("created")     -> created,
              column("updated")     -> updated,
              column("shared")      -> shared,
              column("description") -> folderData.description
            )
        }.update(): Unit

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
        created: NDLADate,
        document: ResourceDocument
    )(implicit session: DBSession = AutoSession): Try[Resource] = Try {
      val jsonDocument = {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(write(document))
        ParameterBinder(dataObject, (ps, idx) => ps.setObject(idx, dataObject))
      }

      val newId  = UUID.randomUUID()
      val column = DBResource.column.c _

      withSQL {
        insert
          .into(DBResource)
          .namedValues(
            column("id")            -> newId,
            column("feide_id")      -> feideId,
            column("path")          -> path,
            column("resource_type") -> resourceType,
            column("created")       -> created,
            column("document")      -> jsonDocument
          )
      }.update(): Unit

      logger.info(s"Inserted new resource with id: $newId")
      document.toFullResource(newId, path, resourceType, feideId, created, None)
    }

    def createFolderResourceConnection(
        folderId: UUID,
        resourceId: UUID,
        rank: Int
    )(implicit session: DBSession = AutoSession): Try[FolderResource] = Try {
      sql"insert into ${DBFolderResource.table} (folder_id, resource_id, rank) values ($folderId, $resourceId, $rank)"
        .update(): Unit
      logger.info(s"Inserted new folder-resource connection with folder id $folderId and resource id $resourceId")

      FolderResource(folderId = folderId, resourceId = resourceId, rank = rank)
    }

    def updateFolder(id: UUID, feideId: FeideID, folder: Folder)(implicit
        session: DBSession = AutoSession
    ): Try[Folder] = Try {
      val column = DBFolder.column.c _
      withSQL {
        update(DBFolder)
          .set(
            column("name")        -> folder.name,
            column("status")      -> folder.status.toString,
            column("shared")      -> folder.shared,
            column("updated")     -> folder.updated,
            column("description") -> folder.description
          )
          .where
          .eq(column("id"), id)
          .and
          .eq(column("feide_id"), feideId)
      }.update()
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
      val newSharedValue = if (newStatus == FolderStatus.SHARED) Some(clock.now()) else None
      val column         = DBFolder.column.c _
      withSQL {
        update(DBFolder)
          .set(
            column("status") -> newStatus.toString,
            column("shared") -> newSharedValue
          )
          .where
          .in(column("id"), folderIds)
      }.update()
    } match {
      case Failure(ex) =>
        Failure(ex)
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

    def deleteAllUserFolders(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[Int] = {
      Try(sql"delete from ${DBFolder.table} where feide_id = $feideId".update()) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) =>
          logger.info(s"Deleted $numRows folders with feide_id = $feideId")
          Success(numRows)
      }
    }

    def deleteAllUserResources(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[Int] = {
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

    def buildTreeStructureFromListOfChildren(
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

    def getFolderAndChildrenSubfoldersWithResources(id: UUID, status: FolderStatus.Value, feideId: Option[FeideID])(
        implicit session: DBSession
    ): Try[Option[Folder]] = {
      feideId match {
        case None => getFolderAndChildrenSubfoldersWithResourcesWhere(id, sqls"AND child.status = ${status.toString}")
        case Some(value) =>
          getFolderAndChildrenSubfoldersWithResourcesWhere(
            id,
            sqls"AND (child.status = ${status.toString} OR child.feide_id = ${value})"
          )
      }
    }

    /** A flat list of the folder with `id` as well as its children folders. The folders in the list comes with
      * connected resources in the `data` list.
      */
    private[repository] def getFolderAndChildrenSubfoldersWithResourcesWhere(id: UUID, sqlFilterClause: SQLSyntax)(
        implicit session: DBSession
    ): Try[Option[Folder]] = Try {
      sql"""-- Big recursive block which fetches the folder with `id` and also its children recursively
            WITH RECURSIVE childs AS (
                SELECT id AS f_id, parent_id AS f_parent_id, feide_id AS f_feide_id, name as f_name, status as f_status, rank AS f_rank, created as f_created, updated as f_updated, shared as f_shared, description as f_description
                FROM ${DBFolder.table} parent
                WHERE id = $id
                UNION ALL
                SELECT child.id AS f_id, child.parent_id AS f_parent_id, child.feide_id AS f_feide_id, child.name AS f_name, child.status as f_status, child.rank AS f_rank, child.created as f_created, child.updated as f_updated, child.shared as f_shared, child.description as f_description
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

    def numberOfSharedFolders()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${DBFolder.table} where status = ${FolderStatus.SHARED.toString}"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfResourcesGrouped()(implicit session: DBSession = ReadOnlyAutoSession): List[(Long, String)] = {
      sql"select count(*) as antall, resource_type from ${DBResource.table} group by resource_type"
        .map(rs => (rs.long("antall"), rs.string("resource_type")))
        .list()
    }
    def getAllFolderRows(implicit session: DBSession): List[FolderRow] = {
      sql"select * from ${DBFolder.table}"
        .map(rs => {
          FolderRow(
            id = UUID.fromString(rs.string("id")),
            parent_id = rs.stringOpt("parent_id").map(x => UUID.fromString(x)),
            feide_id = rs.stringOpt("feide_id"),
            rank = rs.longOpt("rank"),
            name = rs.string("name"),
            status = rs.string("status"),
            created = NDLADate.fromUtcDate(rs.localDateTime("created")),
            updated = NDLADate.fromUtcDate(rs.localDateTime("updated")),
            shared = rs.localDateTimeOpt("shared").map(d => NDLADate.fromUtcDate(d)),
            description = rs.stringOpt("description")
          )
        })
        .list()
    }

    case class ResourceRow(
        id: UUID,
        feide_id: String,
        path: String,
        resource_type: String,
        created: NDLADate,
        document: String
    )
    def getAllResourceRows(implicit session: DBSession): List[ResourceRow] = {
      sql"select * from ${DBResource.table}"
        .map(rs => {
          ResourceRow(
            id = UUID.fromString(rs.string("id")),
            feide_id = rs.string("feide_id"),
            path = rs.string("path"),
            resource_type = rs.string("resource_type"),
            created = NDLADate.fromUtcDate(rs.localDateTime("created")),
            document = rs.string("document")
          )
        })
        .list()
    }

    case class FolderResourceRow(folder_id: UUID, resource_id: UUID, rank: Int)
    def getAllFolderResourceRows(implicit session: DBSession): List[FolderResourceRow] = {
      sql"select * from ${DBFolderResource.table}"
        .map(rs => {
          FolderResourceRow(
            folder_id = UUID.fromString(rs.string("folder_id")),
            resource_id = UUID.fromString(rs.string("resource_id")),
            rank = rs.int("rank")
          )
        })
        .list()
    }

    def insertFolderRow(folderRow: FolderRow)(implicit session: DBSession): Unit = {
      val column = DBFolder.column.c _
      withSQL {
        insert
          .into(DBFolder)
          .namedValues(
            column("id")          -> folderRow.id,
            column("parent_id")   -> folderRow.parent_id,
            column("feide_id")    -> folderRow.feide_id,
            column("rank")        -> folderRow.rank,
            column("name")        -> folderRow.name,
            column("status")      -> folderRow.status,
            column("created")     -> folderRow.created,
            column("updated")     -> folderRow.updated,
            column("shared")      -> folderRow.shared,
            column("description") -> folderRow.description
          )
      }.update(): Unit
      logger.info(s"Inserted new folder with id ${folderRow.id}")
    }

    def insertResourceRow(resourceRow: ResourceRow)(implicit session: DBSession): Unit = {
      val jsonDocument = {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(resourceRow.document)
        ParameterBinder(dataObject, (ps, idx) => ps.setObject(idx, dataObject))
      }
      val column = DBResource.column.c _
      withSQL {
        insert
          .into(DBResource)
          .namedValues(
            column("id")            -> resourceRow.id,
            column("feide_id")      -> resourceRow.feide_id,
            column("path")          -> resourceRow.path,
            column("resource_type") -> resourceRow.resource_type,
            column("created")       -> resourceRow.created,
            column("document")      -> jsonDocument
          )
      }.update(): Unit
      logger.info(s"Inserted new resource with id ${resourceRow.id}")
    }

    def insertFolderResourceRow(folderResourceRow: FolderResourceRow)(implicit session: DBSession): Unit = {
      val column = DBFolderResource.column.c _
      withSQL {
        insert
          .into(DBFolderResource)
          .namedValues(
            column("folder_id")   -> folderResourceRow.folder_id,
            column("resource_id") -> folderResourceRow.resource_id,
            column("rank")        -> folderResourceRow.rank
          )
      }.update(): Unit
      logger.info(
        s"Inserted new folder resource with id ${folderResourceRow.folder_id}_${folderResourceRow.resource_id}"
      )
    }
  }
}

case class FolderRow(
    id: UUID,
    parent_id: Option[UUID],
    feide_id: Option[String],
    rank: Option[Long],
    name: String,
    status: String,
    created: NDLADate,
    updated: NDLADate,
    shared: Option[NDLADate],
    description: Option[String]
)

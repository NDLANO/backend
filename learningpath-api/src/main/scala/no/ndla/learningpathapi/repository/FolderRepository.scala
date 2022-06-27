/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.LazyLogging
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

trait FolderRepository {
  this: DataSource with DBFolder with DBResource with DBFolderResource =>
  val folderRepository: FolderRepository

  class FolderRepository extends LazyLogging {
    implicit val formats: Formats = DBFolder.repositorySerializer + DBResource.JSonSerializer

    def getSession(readOnly: Boolean): DBSession =
      if (readOnly) ReadOnlyAutoSession
      else AutoSession

    def insertFolder(feideId: FeideID, parentId: Option[UUID], document: FolderDocument)(implicit
        session: DBSession = AutoSession
    ): Try[Folder] =
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(write(document))

        val newId = UUID.randomUUID()

        sql"""
        insert into ${DBFolder.table} (id, parent_id, feide_id, document)
        values ($newId, $parentId, $feideId, $dataObject)
        """.update()

        logger.info(s"Inserted new folder with id: $newId")
        document.toFullFolder(newId, feideId, parentId)
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
      document.toFullResource(newId, path, resourceType, feideId, created)
    }

    def createFolderResourceConnection(folderId: UUID, resourceId: UUID)(implicit
        session: DBSession = AutoSession
    ): Try[Unit] = Try {
      sql"insert into ${DBFolderResource.table} (folder_id, resource_id) values ($folderId, $resourceId)".update()
      logger.info(s"Inserted new folder-resource connection with folder id $folderId and resource id $resourceId")
    }

    def updateFolder(id: UUID, feideId: FeideID, folder: Folder)(implicit
        session: DBSession = AutoSession
    ): Try[Folder] = Try {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(folder))

      sql"""
          update ${DBFolder.table}
          set parent_id=${folder.parentId},
              document=$dataObject
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

    def isConnected(folderId: UUID, resourceId: UUID)(implicit session: DBSession = AutoSession): Try[Boolean] = {
      val count: Try[Long] = Try(
        sql"select count(*) from ${DBFolderResource.table} where resource_id=$resourceId and folder_id=$folderId"
          .map(rs => rs.long("count"))
          .single()
          .getOrElse(0)
      )

      count.map(c => c > 0)
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

    def foldersWithParentID(parentId: Option[UUID])(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Try[List[Folder]] =
      foldersWhere(sqls"f.parent_id=$parentId")

    def getFolderResources(
        folderId: UUID
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[Resource]] = Try {
      val fr = DBFolderResource.syntax("fr")
      val r  = DBResource.syntax("r")
      sql"""select ${r.result.*} from ${DBFolderResource.as(fr)}
            left join ${DBResource.as(r)}
                on ${fr.resource_id} = ${r.id}
            where ${fr.folder_id} = $folderId;
           """
        .map(DBResource.fromResultSet(r))
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
        .map(DBResource.fromResultSet(r))
        .list()
        .sequence
    }.flatten

    private def resourceWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[Option[Resource]] = Try {
      val r = DBResource.syntax("r")
      sql"select ${r.result.*} from ${DBResource.as(r)} where $whereClause"
        .map(DBResource.fromResultSet(r))
        .single()
        .sequence
    }.flatten
  }
}

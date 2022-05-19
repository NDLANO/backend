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

import scala.util.{Failure, Success, Try}

trait FolderRepository {
  this: DataSource with DBFolder with DBResource with DBFolderResource =>
  val folderRepository: FolderRepository

  class FolderRepository extends LazyLogging {
    implicit val formats: Formats = DBFolder.repositorySerializer

    def insertFolder(folder: Folder, feideId: FeideID)(implicit
        session: DBSession = AutoSession
    ): Try[Folder] = {
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(write(folder))

        val folderId: Long =
          sql"""
        insert into ${DBFolder.table} (parent_id, feide_id, document) values (${folder.parentId}, $feideId, $dataObject)
        """.updateAndReturnGeneratedKey()

        logger.info(s"Inserted new folder with id: $folderId")
        folder.copy(
          id = Some(folderId),
          feideId = Some(feideId)
        )
      }
    }

    def insertResource(resource: Resource, feideId: FeideID)(implicit
        session: DBSession = AutoSession
    ): Try[Resource] = {
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(write(resource))

        val resourceId: Long =
          sql"""
        insert into ${DBResource.table} (feide_id, document) values ($feideId, $dataObject)
        """.updateAndReturnGeneratedKey()

        logger.info(s"Inserted new resource with id: $resourceId")
        resource.copy(id = Some(resourceId))
      }
    }

    def createFolderResourceConnection(folderId: Long, resourceId: Long)(implicit
        session: DBSession = AutoSession
    ): Try[_] = {
      Try {
        sql"""
        insert into ${DBFolderResource.table} (folder_id, resource_id) values ($folderId, $resourceId)
        """.update()

        logger.info(s"Inserted new folder-resource connection with folder id $folderId and resource id $resourceId")
      }
    }

    def updateFolder(id: Long, feideId: FeideID, folder: Folder)(implicit
        session: DBSession = AutoSession
    ): Try[Folder] = Try {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(folder))

      sql"""
          update ${DBFolder.table}
          set parent_id=${folder.parentId},
              document=$dataObject
          where id=${id} and feide_id=${feideId}
      """.update()

      logger.info(s"Updated folder with id ${id}")
      folder
    }

    def canResourceBeDeleted(resourceId: Long)(implicit session: DBSession = AutoSession): Try[Boolean] = {
      Try(sql"select count(*) from ${DBFolderResource.table} where resource_id = $resourceId".update()) match {
        case Failure(ex)                  => Failure(ex)
        case Success(count) if count != 0 => Success(false)
        case Success(_)                   => Success(true)
      }
    }

    def deleteFolder(id: Long)(implicit session: DBSession = AutoSession): Try[Long] = {
      Try(sql"delete from ${DBFolder.table} where id = $id".update()) match {
        case Failure(ex)                      => Failure(ex)
        case Success(numRows) if numRows != 1 => Failure(NotFoundException(s"Folder with id $id does not exist"))
        case Success(_)                       => Success(id)
      }
    }

    def deleteResource(id: Long)(implicit session: DBSession = AutoSession): Try[Long] = {
      Try(sql"delete from ${DBResource.table} where id = $id".update()) match {
        case Failure(ex)                      => Failure(ex)
        case Success(numRows) if numRows != 1 => Failure(NotFoundException(s"Resource with id $id does not exist"))
        case Success(_)                       => Success(id)
      }
    }

    def folderWithId(id: Long): Try[Folder] = {
      folderWhere(sqls"f.id=$id").flatMap {
        case None         => Failure(NotFoundException(s"Folder with id $id does not exist"))
        case Some(folder) => Success(folder)
      }
    }

    def folderWithFeideId(id: Long, feideId: FeideID): Try[Folder] = {
      folderWhere(sqls"f.id=${id} and f.feide_id=${feideId}").flatMap {
        case None         => Failure(NotFoundException(s"Folder with id $id does not exist"))
        case Some(folder) => Success(folder)
      }
    }

    def resourcesWithFeideId(feideId: FeideID): Try[List[Resource]] = resourcesWhere(sqls"r.feide_id=$feideId")
    def resourceWithResourceAndFeideId(resourceId: Long, feideId: FeideID): Try[Option[Resource]] = {
      resourceWhere(sqls"document->>'resourceId'=${resourceId.toString} and feide_id=$feideId")
    }

    def foldersWithFeideAndParentID(parentId: Option[Long], feideId: FeideID): Try[List[Folder]] = {
      val parentIdClause = parentId match {
        case Some(pid) => sqls"f.parent_id=$pid"
        case None      => sqls"f.parent_id is null"
      }
      foldersWhere(sqls"$parentIdClause and f.feide_id=$feideId")
    }

    def foldersWithParentID(parentId: Option[Long]): Try[List[Folder]] =
      foldersWhere(sqls"f.parent_id=$parentId")

    def getFolderResources(
        id: Long
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[Resource]] = Try {
      val fr = DBFolderResource.syntax("fr")
      val r  = DBResource.syntax("r")
      sql"""select ${r.result.*} from ${DBFolderResource.as(fr)}
            left join ${DBResource.as(r)}
                on fr.resource_id = r.id
            where fr.folder_id = ${id};
           """
        .map(DBResource.fromResultSet(r))
        .list()
    }

    private def folderWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[Folder]] = Try {
      val f = DBFolder.syntax("f")
      sql"select ${f.result.*} from ${DBFolder.as(f)} where $whereClause"
        .map(DBFolder.fromResultSet(f))
        .single()
    }

    private def foldersWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[Folder]] = Try {
      val f = DBFolder.syntax("f")
      sql"select ${f.result.*} from ${DBFolder.as(f)} where $whereClause"
        .map(DBFolder.fromResultSet(f))
        .list()
    }

    private def resourcesWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[Resource]] = Try {
      val r = DBResource.syntax("r")
      sql"select ${r.result.*} from ${DBResource.as(r)} where $whereClause"
        .map(DBResource.fromResultSet(r))
        .list()
    }

    private def resourceWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[Resource]] = Try {
      val r = DBResource.syntax("r")
      sql"select ${r.result.*} from ${DBResource.as(r)} where $whereClause"
        .map(DBResource.fromResultSet(r))
        .single()
    }
  }
}

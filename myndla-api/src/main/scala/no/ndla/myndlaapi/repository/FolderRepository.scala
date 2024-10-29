/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.repository

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.{NotFoundException, RollbackException}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.{FolderStatus, MyNDLAUser}
import no.ndla.common.{CirceUtil, Clock}
import no.ndla.myndlaapi.{maybeUuidBinder, uuidBinder, uuidParameterFactory}
import no.ndla.myndlaapi.model.domain.{
  DBMyNDLAUser,
  Folder,
  FolderResource,
  NDLASQLException,
  NewFolderData,
  Resource,
  ResourceDocument,
  SavedSharedFolder
}
import no.ndla.network.model.FeideID
import org.postgresql.util.PGobject
import scalikejdbc.*
import scalikejdbc.interpolation.SQLSyntax

import java.util.UUID
import scala.util.{Failure, Success, Try}

trait FolderRepository {
  this: Clock =>
  val folderRepository: FolderRepository

  class FolderRepository extends StrictLogging {
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

        val column = Folder.column.c _
        withSQL {
          insert
            .into(Folder)
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
          shared = shared,
          user = None
        )
      }

    def insertResource(
        feideId: FeideID,
        path: String,
        resourceType: ResourceType,
        created: NDLADate,
        document: ResourceDocument
    )(implicit session: DBSession = AutoSession): Try[Resource] = Try {
      val jsonDocument = {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(CirceUtil.toJsonString(document))
        ParameterBinder(dataObject, (ps, idx) => ps.setObject(idx, dataObject))
      }

      val newId  = UUID.randomUUID()
      val column = Resource.column.c _

      withSQL {
        insert
          .into(Resource)
          .namedValues(
            column("id")            -> newId,
            column("feide_id")      -> feideId,
            column("path")          -> path,
            column("resource_type") -> resourceType.entryName,
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
        rank: Int,
        favoritedDate: NDLADate
    )(implicit session: DBSession = AutoSession): Try[FolderResource] = Try {
      withSQL {
        insert
          .into(FolderResource)
          .namedValues(
            FolderResource.column.folderId      -> folderId,
            FolderResource.column.resourceId    -> resourceId,
            FolderResource.column.rank          -> rank,
            FolderResource.column.favoritedDate -> favoritedDate
          )
      }.update(): Unit
      logger.info(s"Inserted new folder-resource connection with folder id $folderId and resource id $resourceId")

      FolderResource(folderId = folderId, resourceId = resourceId, rank = rank, favoritedDate = favoritedDate)
    }

    def updateFolder(id: UUID, feideId: FeideID, folder: Folder)(implicit
        session: DBSession = AutoSession
    ): Try[Folder] = Try {
      val column = Folder.column.c _
      withSQL {
        update(Folder)
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
      val column         = Folder.column.c _
      withSQL {
        update(Folder)
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
      dataObject.setValue(CirceUtil.toJsonString(resource))

      sql"""
          update ${Resource.table}
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
        sql"select count(*) from ${FolderResource.table} where resource_id=$resourceId"
          .map(rs => rs.long("count"))
          .single()
          .getOrElse(0)
      )
    }

    def getConnection(folderId: UUID, resourceId: UUID)(implicit
        session: DBSession = AutoSession
    ): Try[Option[FolderResource]] = {
      Try(
        sql"select resource_id, folder_id, rank, favorited_date from ${FolderResource.table} where resource_id=$resourceId and folder_id=$folderId"
          .map(rs => {
            for {
              resourceId <- rs.get[Try[UUID]]("resource_id")
              folderId   <- rs.get[Try[UUID]]("folder_id")
              rank          = rs.int("rank")
              favoritedDate = NDLADate.fromUtcDate(rs.localDateTime("favorited_date"))
            } yield FolderResource(resourceId, folderId, rank, favoritedDate)
          })
          .single()
      )
        .map(_.sequence)
        .flatten
    }

    def getConnections(folderId: UUID)(implicit session: DBSession = AutoSession): Try[List[FolderResource]] = {
      Try(
        sql"select resource_id, folder_id, rank, favorited_date from ${FolderResource.table} where folder_id=$folderId"
          .map(rs => {
            for {
              resourceId <- rs.get[Try[UUID]]("resource_id")
              folderId   <- rs.get[Try[UUID]]("folder_id")
              rank          = rs.int("rank")
              favoritedDate = NDLADate.fromUtcDate(rs.localDateTime("favorited_date"))
            } yield FolderResource(folderId, resourceId, rank, favoritedDate)
          })
          .list()
      )
        .map(_.sequence)
        .flatten
    }

    def deleteFolder(id: UUID)(implicit session: DBSession = AutoSession): Try[UUID] = {
      Try(sql"delete from ${Folder.table} where id = $id".update()) match {
        case Failure(ex)                      => Failure(ex)
        case Success(numRows) if numRows != 1 => Failure(NotFoundException(s"Folder with id $id does not exist"))
        case Success(_) =>
          logger.info(s"Deleted folder with id $id")
          Success(id)
      }
    }

    def deleteResource(id: UUID)(implicit session: DBSession = AutoSession): Try[UUID] = {
      Try(sql"delete from ${Resource.table} where id = $id".update()) match {
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
        sql"delete from ${FolderResource.table} where folder_id=$folderId and resource_id=$resourceId".update()
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

    def getAllFavorites(implicit session: DBSession): Try[Map[String, Map[String, Long]]] = Try {
      sql"""
          select count(*) as count, document->>'resourceId' as resource_id, resource_type
          from ${FolderResource.table} fr
          inner join ${Resource.table} r on fr.resource_id = r.id
          group by document->>'resourceId',resource_type
         """
        .foldLeft(Map.empty[String, Map[String, Long]]) { case (acc, rs) =>
          val count        = rs.long("count")
          val resourceId   = rs.string("resource_id")
          val resourceType = rs.string("resource_type")
          val rtMap        = acc.getOrElse(resourceType, Map.empty)
          val newRtMap     = rtMap + (resourceId -> count)
          acc + (resourceType -> newRtMap)
        }
    }

    def getRecentFavorited(size: Option[Int], excludeResourceTypes: List[ResourceType])(implicit
        session: DBSession = AutoSession
    ): Try[List[Resource]] = Try {
      val fr = FolderResource.syntax("fr")
      val r  = Resource.syntax("r")
      val where =
        if (excludeResourceTypes.nonEmpty) {
          sqls"""where ${r.resourceType} not in (${excludeResourceTypes.map(_.entryName)})"""
        } else { sqls"" }
      sql"""select ${r.result.*}, ${fr.result.*} from ${FolderResource.as(fr)}
            left join ${Resource.as(r)}
                on ${fr.resourceId} = ${r.id}
            $where
            order by favorited_date DESC
            limit ${size.getOrElse(1)}
           """
        .one(Resource.fromResultSet(r, withConnection = false))
        .toOne(rs => FolderResource.fromResultSet(fr)(rs).toOption)
        .map((resource, connection) => resource.map(_.copy(connection = connection)))
        .list()
        .sequence
    }.flatten

    def numberOfFavouritesForResource(resourceId: String, resourceType: String)(implicit
        session: DBSession
    ): Try[Long] = Try {
      sql"""
            select count(*) as count from ${FolderResource.table} fr
            inner join ${Resource.table} r on fr.resource_id = r.id
            where r.document->>'resourceId' = $resourceId
            and r.resource_type = $resourceType
         """
        .map(rs => rs.long("count"))
        .single()
        .getOrElse(0L)
    }

    def deleteAllUserFolders(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[Int] = {
      Try(sql"delete from ${Folder.table} where feide_id = $feideId".update()) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) =>
          logger.info(s"Deleted $numRows folders with feide_id = $feideId")
          Success(numRows)
      }
    }

    def deleteAllUserResources(feideId: FeideID)(implicit session: DBSession = AutoSession): Try[Int] = {
      Try(sql"delete from ${Resource.table} where feide_id = $feideId".update()) match {
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
        resourceType: ResourceType,
        feideId: FeideID
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[Resource]] = resourceWhere(
      sqls"path=$path and resource_type=${resourceType.entryName} and feide_id=$feideId"
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
                FROM ${Folder.table} parent
                WHERE id = $id
                UNION ALL
                SELECT child.id AS f_id, child.parent_id AS f_parent_id, child.feide_id AS f_feide_id, child.name AS f_name, child.status as f_status, child.rank AS f_rank, child.created as f_created, child.updated as f_updated, child.shared as f_shared, child.description as f_description
                FROM ${Folder.table} child
                JOIN childs AS parent ON parent.f_id = child.parent_id
                $sqlFilterClause
            )
            SELECT * FROM childs
            LEFT JOIN folder_resources fr ON fr.folder_id = f_id
            LEFT JOIN ${Resource.table} r ON r.id = fr.resource_id;
         """
        // We prefix the `folders` columns with `f_` to separate them
        // from the `folder_resources` columns  (both here and in sql).
        .one(rs => Folder.fromResultSet(s => s"f_$s")(rs))
        .toMany(rs => Resource.fromResultSetOpt(rs, withConnection = true).sequence)
        .map((folder, resources) =>
          resources.toList.sequence.flatMap(resources => folder.map(f => f.copy(resources = resources)))
        )
        .list()
        .sequence
    }.flatten.map(data => buildTreeStructureFromListOfChildren(id, data))

    def getSharedFolderAndChildrenSubfoldersWithResources(id: UUID)(implicit
        session: DBSession
    ): Try[Option[Folder]] = Try {
      val u   = DBMyNDLAUser.syntax("u")
      val r   = Resource.syntax("r")
      val fr  = FolderResource.syntax("fr")
      val sfu = SavedSharedFolder.syntax("sfu")

      sql"""-- Big recursive block which fetches the folder with `id` and also its children recursively
            WITH RECURSIVE childs AS (
                SELECT id AS f_id, parent_id AS f_parent_id, feide_id AS f_feide_id, name as f_name, status as f_status, rank AS f_rank, created as f_created, updated as f_updated, shared as f_shared, description as f_description
                FROM ${Folder.table} parent
                WHERE id = $id
                UNION ALL
                SELECT child.id AS f_id, child.parent_id AS f_parent_id, child.feide_id AS f_feide_id, child.name AS f_name, child.status as f_status, child.rank AS f_rank, child.created as f_created, child.updated as f_updated, child.shared as f_shared, child.description as f_description
                FROM ${Folder.table} child
                JOIN childs AS parent ON parent.f_id = child.parent_id
                AND child.status = ${FolderStatus.SHARED.toString}
            )
            SELECT childs.*, ${r.resultAll}, ${u.resultAll}, ${fr.resultAll}, ${sfu.resultAll} FROM childs
            LEFT JOIN ${FolderResource.as(fr)} ON ${fr.folderId} = f_id
            LEFT JOIN ${Resource.as(r)} ON ${r.id} = ${fr.resourceId}
            LEFT JOIN ${DBMyNDLAUser.as(u)} on ${u.feideId} = f_feide_id
            LEFT JOIN ${SavedSharedFolder.as(sfu)} on ${sfu.folderId} = f_id;
         """
        .one(rs => Folder.fromResultSet(s => s"f_$s")(rs))
        .toManies(
          rs => Resource.fromResultSetSyntaxProviderWithConnection(r, fr)(rs).sequence,
          rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption,
          rs => Try(SavedSharedFolder.fromResultSet(sfu)(rs)).toOption
        )
        .map((folder, resources, user, savedSharedFolder) => {
          toCompileFolder(folder, resources.toList, user.toList, savedSharedFolder.toList)
        })
        .list()
        .sequence
    }.flatten.map(data => buildTreeStructureFromListOfChildren(id, data))

    private def toCompileFolder(
        folder: Try[Folder],
        resource: Seq[Try[Resource]],
        users: Seq[MyNDLAUser],
        savedSharedFolder: Seq[Try[SavedSharedFolder]]
    ): Try[Folder] =
      for {
        f            <- folder
        resources    <- resource.toList.sequence
        user         <- findUser(f.feideId, users)
        savedFolders <- savedSharedFolder.sequence
        rank         <- findRank(f, savedFolders)
      } yield f.copy(
        rank = rank,
        resources = resources,
        user = user
      )

    private def findRank(folder: Folder, sharedFolderConnections: Seq[SavedSharedFolder]): Try[Int] = {
      sharedFolderConnections.find(_.folderId == folder.id) match {
        case Some(value) => Success(value.rank)
        case None        => Success(folder.rank)
      }
    }

    private def findUser(feideId: FeideID, users: collection.Seq[MyNDLAUser]): Try[Option[MyNDLAUser]] =
      users.find(user => feideId == user.feideId) match {
        case Some(u) => Success(Some(u))
        case None    => Failure(NDLASQLException(s"${feideId} does not match any users with folder"))
      }

    def getFolderAndChildrenSubfolders(id: UUID)(implicit session: DBSession): Try[Option[Folder]] = Try {
      sql"""-- Big recursive block which fetches the folder with `id` and also its children recursively
            WITH RECURSIVE childs AS (
                SELECT parent.*
                FROM ${Folder.table} parent
                WHERE id = $id
                UNION ALL
                SELECT child.*
                FROM ${Folder.table} child
                JOIN childs AS parent ON parent.id = child.parent_id
            )
            SELECT * FROM childs;
         """
        .map(rs => Folder.fromResultSet(rs))
        .list()
        .sequence
    }.flatten.map(data => buildTreeStructureFromListOfChildren(id, data))

    def getFoldersDepth(parentId: UUID)(implicit session: DBSession = ReadOnlyAutoSession): Try[Long] = Try {
      sql"""
           WITH RECURSIVE parents AS (
                SELECT id AS f_id, parent_id AS f_parent_id, 0 dpth
                FROM ${Folder.table} child
                WHERE id = $parentId
                UNION ALL
                SELECT parent.id AS f_id, parent.parent_id AS f_parent_id, dpth +1
                FROM ${Folder.table} parent
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
                  FROM ${Folder.table} child
                  WHERE id = $folderId
                  UNION ALL
                  SELECT child.id
                  FROM ${Folder.table} child, parent
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
      val fr = FolderResource.syntax("fr")
      val r  = Resource.syntax("r")
      sql"""select ${r.result.*}, ${fr.result.*} from ${FolderResource.as(fr)}
            left join ${Resource.as(r)}
                on ${fr.resourceId} = ${r.id}
            where ${fr.folderId} = $folderId;
           """
        .one(Resource.fromResultSet(r, withConnection = false))
        .toOne(rs => FolderResource.fromResultSet(fr)(rs).toOption)
        .map((resource, connection) => resource.map(_.copy(connection = connection)))
        .list()
        .sequence
    }.flatten

    private def folderWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[Option[Folder]] = Try {
      val f = Folder.syntax("f")
      sql"select ${f.result.*} from ${Folder.as(f)} where $whereClause"
        .map(Folder.fromResultSet(f))
        .single()
        .sequence
    }.flatten

    private def foldersWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[List[Folder]] = Try {
      val f = Folder.syntax("f")
      sql"select ${f.result.*} from ${Folder.as(f)} where $whereClause"
        .map(Folder.fromResultSet(f))
        .list()
        .sequence
    }.flatten

    private def resourcesWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[List[Resource]] = Try {
      val r = Resource.syntax("r")
      sql"select ${r.result.*} from ${Resource.as(r)} where $whereClause"
        .map(Resource.fromResultSet(r, withConnection = false))
        .list()
        .sequence
    }.flatten

    private def resourceWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[Option[Resource]] = Try {
      val r = Resource.syntax("r")
      sql"select ${r.result.*} from ${Resource.as(r)} where $whereClause"
        .map(Resource.fromResultSet(r, withConnection = false))
        .single()
        .sequence
    }.flatten

    def setFolderRank(folderId: UUID, rank: Int, feideId: FeideID)(implicit session: DBSession): Try[Unit] = {
      Try {
        sql"""
          update ${Folder.table}
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

    def setSharedFolderRank(folderId: UUID, rank: Int, feideId: FeideID)(implicit session: DBSession): Try[Unit] = {
      Try {
        sql"""
          update ${SavedSharedFolder.table}
          set rank=$rank
          where folder_id=$folderId and feide_id=$feideId
      """
          .update()
      } match {
        case Failure(ex) => Failure(ex)
        case Success(count) if count == 1 =>
          logger.info(s"Updated rank for shared folder with id $folderId and feideId $feideId")
          Success(())
        case Success(count) =>
          Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
      }
    }

    def setResourceConnectionRank(folderId: UUID, resourceId: UUID, rank: Int)(implicit session: DBSession): Try[Unit] =
      Try {
        sql"""
          update ${FolderResource.table}
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
      sql"select count(tag) from (select distinct jsonb_array_elements_text(document->'tags') from ${Resource.table}) as tag"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfResources()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${Resource.table}"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfFolders()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${Folder.table}"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfSharedFolders()(implicit session: DBSession = ReadOnlyAutoSession): Option[Long] = {
      sql"select count(*) from ${Folder.table} where status = ${FolderStatus.SHARED.toString}"
        .map(rs => rs.long("count"))
        .single()
    }

    def numberOfResourcesGrouped()(implicit session: DBSession = ReadOnlyAutoSession): List[(Long, String)] = {
      sql"select count(*) as antall, resource_type from ${Resource.table} group by resource_type"
        .map(rs => (rs.long("antall"), rs.string("resource_type")))
        .list()
    }
    def getAllFolderRows(implicit session: DBSession): List[FolderRow] = {
      sql"select * from ${Folder.table}"
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
      sql"select * from ${Resource.table}"
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
      sql"select * from ${FolderResource.table}"
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
      val column = Folder.column.c _
      withSQL {
        insert
          .into(Folder)
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
      val column = Resource.column.c _
      withSQL {
        insert
          .into(Resource)
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
      val column = FolderResource.column.c _
      withSQL {
        insert
          .into(FolderResource)
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

    def createFolderUserConnection(folderId: UUID, feideId: FeideID, rank: Int)(implicit
        session: DBSession = AutoSession
    ): Try[SavedSharedFolder] = Try {
      withSQL {
        insert
          .into(SavedSharedFolder)
          .namedValues(
            SavedSharedFolder.column.folderId -> folderId,
            SavedSharedFolder.column.feideId  -> feideId,
            SavedSharedFolder.column.rank     -> rank
          )
      }.update(): Unit
      logger.info(s"Inserted new sharedFolder-user connection with folder id $folderId and feide id $feideId")

      SavedSharedFolder(folderId = folderId, feideId = feideId, rank = rank)
    }

    def deleteFolderUserConnections(
        folderIds: List[UUID]
    )(implicit session: DBSession = AutoSession): Try[List[UUID]] = Try {
      val column = SavedSharedFolder.column.c _
      withSQL {
        delete
          .from(SavedSharedFolder)
          .where
          .in(column("folder_id"), folderIds)
      }.update()
    } match {
      case Failure(ex) => Failure(ex)
      case Success(numRows) =>
        logger.info(s"Deleted $numRows shared folder user connections with folder ids (${folderIds.mkString(", ")})")
        Success(folderIds)
    }

    def deleteFolderUserConnection(
        folderId: Option[UUID],
        feideId: Option[FeideID]
    )(implicit session: DBSession = AutoSession): Try[Int] = Try {
      (folderId, feideId) match {
        case (Some(folderId), Some(feideId)) =>
          deleteFolderUserConnectionWhere(sqls"folder_id = $folderId AND feide_id = $feideId")
        case (Some(folderId), None) =>
          deleteFolderUserConnectionWhere(sqls"folder_id = $folderId ")
        case (None, Some(feideId)) =>
          deleteFolderUserConnectionWhere(sqls"feide_id = $feideId")
        case (None, None) => Failure(NDLASQLException("No feide id or folder id provided"))
      }
    }.flatMap {
      case Failure(ex)     => Failure(ex)
      case Success(numRow) => Success(numRow)
    }

    private def deleteFolderUserConnectionWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Try[Int] = {
      val f = SavedSharedFolder.syntax("f")
      Try(
        sql"DELETE FROM ${SavedSharedFolder.as(f)} WHERE $whereClause".update()
      ) match {
        case Failure(ex) => Failure(ex)
        case Success(numRows) =>
          logger.info(s"Deleted $numRows from shared folder user connections")
          Success(numRows)
      }
    }

    def getSavedSharedFolders(
        feideId: FeideID
    )(implicit session: DBSession = AutoSession): Try[List[Folder]] = Try {
      val f   = Folder.syntax("f")
      val sfu = SavedSharedFolder.syntax("sfu")
      sql"""
          SELECT ${f.result.*}, ${sfu.result.*}
          FROM ${Folder.as(f)}
          LEFT JOIN ${SavedSharedFolder.as(sfu)} on sfu.folder_id = f.id
          WHERE sfu.feide_id = $feideId
        """
        .map(rs => {
          val folder = Folder.fromResultSet(f)(rs)
          folder.map {
            val sharedRank = rs.int(sfu.resultName.rank)
            _.copy(rank = sharedRank)
          }
        })
        .list()
        .sequence
    }.flatten
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

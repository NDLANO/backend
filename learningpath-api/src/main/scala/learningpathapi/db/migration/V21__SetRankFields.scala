/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package learningpathapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import scalikejdbc.{DB, DBSession, _}

import java.util.UUID

class V21__SetRankFields extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateFoldersAndResources
    }
  }

  def migrateFoldersAndResources(implicit session: DBSession): Unit = {
    allFeideIds().foreach(feideId => {
      allRootFolders(feideId).zipWithIndex.foreach { case (id, idx) =>
        val rank = idx + 1
        updateFolderRank(id, rank)
        handleFolderAndChildren(id)
      }
    })
  }

  implicit val uuidBinder: Binders[UUID] = Binders.of[UUID] {
    case v: UUID => v
    case _       => throw new RuntimeException("Something went wrong when parsing UUID from database.")
  }(v => (ps, idx) => ps.setObject(idx, v))

  sealed trait FolderContent
  case class Connection(folder_id: UUID, resource_id: UUID) extends FolderContent
  case class Folder(id: UUID)                               extends FolderContent

  def getResourceConnections(folderId: UUID)(implicit session: DBSession): Seq[Connection] = {
    sql"""
         select * from folder_resources where folder_id = $folderId
       """
      .map(rs => {
        Connection(
          rs.get[UUID]("folder_id"),
          rs.get[UUID]("resource_id")
        )
      })
      .list
      .apply()
  }

  def getSubfolders(folderId: UUID)(implicit session: DBSession): List[Folder] =
    sql"select * from folders where parent_id = $folderId"
      .map(rs => Folder(rs.get[UUID]("id")))
      .list
      .apply()

  def updateConnectionRank(folderId: UUID, resourceId: UUID, rank: Int)(implicit session: DBSession): Int = {
    sql"update folder_resources set rank=$rank where folder_id=$folderId and resource_id=$resourceId".update.apply()
  }

  def updateFolderRank(id: UUID, rank: Int)(implicit session: DBSession): Int = {
    sql"update folders set rank=$rank where id=$id".update.apply()
  }

  def handleFolderAndChildren(id: UUID)(implicit session: DBSession): Unit = {
    val subfolders          = getSubfolders(id)
    val resourceConnections = getResourceConnections(id)

    resourceConnections.zipWithIndex.foreach { case (res, idx) =>
      val rank = idx + 1
      updateConnectionRank(res.folder_id, res.resource_id, rank)
    }

    subfolders.zipWithIndex.foreach { case (res, idx) =>
      val rank = idx + 1
      updateFolderRank(res.id, rank)
      handleFolderAndChildren(res.id)
    }
  }

  def allFeideIds()(implicit session: DBSession): Seq[String] = {
    sql"select distinct feide_id from folders where parent_id is null;"
      .map(rs => rs.string("feide_id"))
      .list()
  }

  def allRootFolders(feideId: String)(implicit session: DBSession): Seq[UUID] = {
    sql"select id from folders where parent_id is null and feide_id = $feideId"
      .map(rs => rs.get[UUID]("id"))
      .list()
  }
}

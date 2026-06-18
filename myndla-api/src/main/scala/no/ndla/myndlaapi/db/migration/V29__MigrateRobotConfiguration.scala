/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.db.migration

import io.circe.{Json, parser}
import no.ndla.database.{TableIdType, TableMigration}
import org.postgresql.util.PGobject
import scalikejdbc.interpolation.Implicits.scalikejdbcSQLInterpolationImplicitDef
import scalikejdbc.{DBSession, SQLSyntax, WrappedResultSet}

import java.util.UUID

case class RobotDocumentRow(id: UUID, configuration: String)

class V29__MigrateRobotConfiguration extends TableMigration[RobotDocumentRow] {
  private val columnName: String        = "configuration"
  override val tableName: String        = "robot_definitions"
  override val tableIdType: TableIdType = TableIdType.UUID

  private lazy val columnNameSQL: SQLSyntax = SQLSyntax.createUnsafely(columnName)
  override lazy val whereClause: SQLSyntax  = sqls"$columnNameSQL is not null"

  override def extractRowData(rs: WrappedResultSet): RobotDocumentRow =
    RobotDocumentRow(rs.any("id").asInstanceOf[UUID], rs.string(columnName))

  override def updateRow(rowData: RobotDocumentRow)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    val newDocument = convertColumn(rowData.configuration)
    dataObject.setValue(newDocument)
    sql"""update $tableNameSQL
          set $columnNameSQL = $dataObject
          where id = ${rowData.id}
       """.update()
  }

  def convertColumn(document: String): String = {
    val oldDoc = parser.parse(document).toTry.get
    oldDoc.asObject match {
      case None       => document
      case Some(root) =>
        // Extract title from root level (old format had it there)
        val title = root("title").getOrElse(Json.fromString(""))

        // Remove title from root
        val rootWithoutTitle = root.remove("title")

        // Transform settings object
        val newSettings = rootWithoutTitle("settings").flatMap(_.asObject) match {
          case None           => Json.obj()
          case Some(settings) =>
            // systemprompt and question were Option[String], default null to empty string
            val systemprompt = settings("systemprompt")
              .flatMap(_.asString)
              .map(Json.fromString)
              .getOrElse(Json.fromString(""))
            val question = settings("question").flatMap(_.asString).map(Json.fromString).getOrElse(Json.fromString(""))

            Json.fromJsonObject(
              settings
                .add("title", title)
                .add("description", settings("description").getOrElse(Json.Null))
                .add("systemprompt", systemprompt)
                .add("question", question)
                .add("voice", settings("voice").getOrElse(Json.fromString("")))
            )
        }

        Json.fromJsonObject(rootWithoutTitle.add("settings", newSettings)).noSpaces
    }
  }

}

/*
 * Part of NDLA draft-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import io.circe.{Json, parser}
import no.ndla.database.TableMigration
import org.postgresql.util.PGobject
import scalikejdbc.{DBSession, SQLSyntax, WrappedResultSet}
import scalikejdbc.interpolation.Implicits.scalikejdbcSQLInterpolationImplicitDef

case class DocumentRow(id: Long, revision: Int, articleId: Long, document: String)

class V83__SetPublishedCount extends TableMigration[DocumentRow] {
  val columnName: String         = "document"
  override val tableName: String = "articledata"

  private lazy val columnNameSQL: SQLSyntax = SQLSyntax.createUnsafely(columnName)
  override lazy val whereClause: SQLSyntax  = sqls"$columnNameSQL is not null"

  private def countOtherVersions(revision: Int, articleId: Long)(implicit session: DBSession): Long = {
    sql"select count(*) from $tableNameSQL where revision < $revision and article_id = $articleId and $columnNameSQL -> 'status' ->> 'current' = 'PUBLISHED'"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0L)
  }

  override def extractRowData(rs: WrappedResultSet): DocumentRow =
    DocumentRow(rs.long("id"), rs.int("revision"), rs.long("article_id"), rs.string(columnName))

  override def updateRow(rowData: DocumentRow)(implicit session: DBSession): Int = {
    var other       = countOtherVersions(rowData.revision, rowData.articleId)
    val oldDocument = parser.parse(rowData.document).toTry.get
    val published   = oldDocument.hcursor.downField("status").downField("current").as[String].toTry.getOrElse("")
    if (published == "PUBLISHED") {
      other = other + 1
    }
    val newDoc = oldDocument.mapObject(_.add("publishedCount", Json.fromLong(other))).noSpaces

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(newDoc)
    sql"""update $tableNameSQL
          set $columnNameSQL = $dataObject
          where id = ${rowData.id}
       """.update()
  }
}

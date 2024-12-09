/*
 * Part of NDLA database
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import scalikejdbc.*

abstract class TableMigration[ROW_DATA] extends BaseJavaMigration {
  val tableName: String
  val whereClause: SQLSyntax
  val chunkSize: Int = 1000
  def extractRowData(rs: WrappedResultSet): ROW_DATA
  def updateRow(rowData: ROW_DATA)(implicit session: DBSession): Int
  lazy val tableNameSQL: SQLSyntax = SQLSyntax.createUnsafely(tableName)

  private def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from $tableNameSQL where $whereClause"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allRows(offset: Long)(implicit session: DBSession): Seq[ROW_DATA] = {
    sql"select * from $tableNameSQL where $whereClause order by id limit $chunkSize offset $offset"
      .map(rs => extractRowData(rs))
      .list()
  }

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session => migrateRows(session) }

  private def migrateRows(implicit session: DBSession): Unit = {
    val count        = countAllRows.get
    var numPagesLeft = (count / chunkSize) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allRows(offset * chunkSize).map { rowData => updateRow(rowData) }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }
}

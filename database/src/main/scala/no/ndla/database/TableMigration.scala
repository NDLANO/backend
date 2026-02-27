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

/** Base class for Scala-based migrations.
  *
  * **NOTE:** If the table you are migrating does not use `bigint` as the ID type, you must override `tableIdType` to
  * return the correct [[TableIdType]].
  */
abstract class TableMigration[ROW_DATA] extends BaseJavaMigration {
  val tableName: String
  lazy val whereClause: SQLSyntax
  val chunkSize: Int = 1000
  def extractRowData(rs: WrappedResultSet): ROW_DATA
  def updateRow(rowData: ROW_DATA)(implicit session: DBSession): Int
  lazy val tableNameSQL: SQLSyntax = SQLSyntax.createUnsafely(tableName)
  val tableIdType: TableIdType     = TableIdType.Bigint

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session =>
      migrateRows(using session)
    }

  protected def migrateRows(implicit session: DBSession): Unit = Iterator
    .unfold(tableIdType.zeroValueScala) { lastId =>
      getRowChunk(lastId) match {
        case Nil   => None
        case chunk => Some((chunk, chunk.last._1))
      }
    }
    .takeWhile(_.nonEmpty)
    .foreach { chunk =>
      chunk.foreach((_, rowData) => updateRow(rowData))
    }

  private def getRowChunk(
      lastId: tableIdType.ScalaType
  )(implicit session: DBSession): Seq[(tableIdType.ScalaType, ROW_DATA)] = {
    sql"select * from $tableNameSQL where $whereClause and id > $lastId order by id limit $chunkSize"
      .map(rs => (tableIdType.fromResultSet(rs), extractRowData(rs)))
      .list()
  }
}

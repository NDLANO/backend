/*
 * Part of NDLA database
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, UnitTestSuite}
import org.flywaydb.core.Flyway
import scalikejdbc.*

import java.util.UUID

class TableMigrationTest extends DatabaseIntegrationSuite, UnitTestSuite, TestEnvironment {
  val dataSource: DataSource      = testDataSource.get
  val schema: String              = "testschema"
  val schemaSql: SQLSyntax        = SQLSyntax.createUnsafely(schema)
  val intTableName: String        = "test"
  val intTableNameSql: SQLSyntax  = SQLSyntax.createUnsafely(intTableName)
  val uuidTableName: String       = "test2"
  val uuidTableNameSql: SQLSyntax = SQLSyntax.createUnsafely(uuidTableName)

  override def beforeAll(): Unit = {
    super.beforeAll()

    dataSource.connectToDatabase()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    DB.autoCommit { implicit session =>
      sql"""
            drop schema if exists $schemaSql cascade;
            create schema $schemaSql;
            create table $intTableNameSql (id int primary key, data text);
            create table $uuidTableNameSql (id uuid primary key, data text);""".execute()
    }
  }

  private def insertIdsFromRange(range: Range): Unit = {
    DB.autoCommit { implicit session =>
      val sqlInsertParts  = range.map(id => sqls"insert into $intTableNameSql (id, data) values ($id, ${"row" + id})")
      val joinedSqlInsert = SQLSyntax.join(sqlInsertParts, sqls";")
      sql"$joinedSqlInsert".execute()
    }
  }

  private def runMigration[A](migration: TableMigration[A]): Unit = {
    val flyway = Flyway
      .configure()
      .javaMigrations(migration)
      .dataSource(dataSource)
      .schemas(schema)
      .baselineVersion("00")
      .baselineOnMigrate(true)
      .load()

    flyway.migrate()
  }

  test("that all rows are updated with no where clause") {
    insertIdsFromRange(1 to 50)

    class V01__Foo extends TableMigration[Long] {
      override val tableName: String           = intTableName
      override lazy val whereClause: SQLSyntax = sqls"true"
      override val chunkSize: Int              = 10

      override def extractRowData(rs: WrappedResultSet): Long = rs.long("id")

      override def updateRow(rowData: Long)(implicit session: DBSession): Int = {
        sql"update $intTableNameSql set data = ${"updated_row" + rowData} where id = $rowData".update()
      }
    }

    runMigration(V01__Foo())

    DB.readOnly { implicit session =>
      val updatedRowsCount = sql"select count(*) from $intTableNameSql where data like 'updated_row%'"
        .map(_.int(1))
        .single()
        .get
      updatedRowsCount should be(50)
    }
  }

  test("that keyset pagination works correctly") {
    val step = 3
    insertIdsFromRange(100 to 1 by -step)
    val maxIdToUpdate       = 50
    val expectedUpdateCount = (maxIdToUpdate / step) + 1

    class V01__Foo extends TableMigration[Long] {
      override val tableName: String           = intTableName
      override lazy val whereClause: SQLSyntax = sqls"id < $maxIdToUpdate"
      override val chunkSize: Int              = 10

      override def extractRowData(rs: WrappedResultSet): Long = rs.long("id")

      override def updateRow(rowData: Long)(implicit session: DBSession): Int = {
        sql"update $intTableNameSql set data = ${"updated_row" + rowData} where id = $rowData".update()
      }
    }

    runMigration(V01__Foo())

    DB.readOnly { implicit session =>
      val updatedIds = sql"select id from $intTableNameSql where data like 'updated_row%' order by id"
        .map(_.int("id"))
        .list()
      all(updatedIds) should be < maxIdToUpdate
      updatedIds.length should be(expectedUpdateCount)
    }
  }

  test("that migration works with UUIDs as primary keys") {
    val numRows = 50
    DB.autoCommit { implicit session =>
      val sqlInsertParts = (
        1 to numRows
      ).map { i =>
        val uuid = UUID.randomUUID()
        sqls"insert into $uuidTableNameSql (id, data) values ($uuid, ${"row" + i})"
      }
      val joinedSqlInsert = SQLSyntax.join(sqlInsertParts, sqls";")
      sql"$joinedSqlInsert".execute()
    }

    class V01__Foo extends TableMigration[(UUID, String)] {
      override val tableName: String           = uuidTableName
      override lazy val whereClause: SQLSyntax = sqls"true"
      override val chunkSize: Int              = 10
      override val tableIdType: TableIdType    = TableIdType.UUID

      override def extractRowData(rs: WrappedResultSet): (UUID, String) =
        (UUID.fromString(rs.string("id")), rs.string("data"))

      override def updateRow(rowData: (UUID, String))(implicit session: DBSession): Int = {
        sql"update $uuidTableNameSql set data = ${"updated_row " + rowData._2} where id = ${rowData._1}".update()
      }
    }

    runMigration(V01__Foo())

    DB.readOnly { implicit session =>
      val updatedRowsCount = sql"select count(*) from $uuidTableNameSql where data like 'updated_row %'"
        .map(_.int(1))
        .single()
        .get
      updatedRowsCount should be(numRows)
    }
  }
}

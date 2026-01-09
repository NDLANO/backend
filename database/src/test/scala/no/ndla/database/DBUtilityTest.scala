/*
 * Part of NDLA database
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, UnitTestSuite}
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

class DBUtilityTest extends DatabaseIntegrationSuite, UnitTestSuite, TestEnvironment {
  val dataSource: DataSource = testDataSource.get

  override def beforeAll(): Unit = {
    super.beforeAll()

    dataSource.connectToDatabase()

    DB.autoCommit { implicit session =>
      sql"""
            create schema if not exists testschema;
            create table test (id int primary key, data text);
            create table test_json (id int primary key, payload jsonb);""".execute()
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    DB.autoCommit { implicit session =>
      sql"""
            delete from test;
            delete from test_json;""".execute()
    }
  }

  test("rollbackOnFailure rolls back changes on Failure") {
    val expected = new RuntimeException("rollback requested")
    val result   = dbUtil.rollbackOnFailure { implicit session =>
      sql"insert into test (id, data) values (1, 'should_rollback')".update()
      Failure(expected)
    }

    result should be(Failure(expected))

    val stored = dbUtil.readOnly { implicit session =>
      sql"select * from test where id = 1".map(_.string("data")).single()
    }
    stored should be(None)
  }

  test("rollbackOnFailure commits changes on Success") {
    val result = dbUtil.rollbackOnFailure { implicit session =>
      sql"insert into test (id, data) values (1, 'committed')".update()
      Success("ok")
    }

    result should be(Success("ok"))

    val stored = dbUtil.readOnly { implicit session =>
      sql"select * from test where id = 1".map(_.string("data")).single()
    }
    stored should be(Some("committed"))
  }

  test("writeSession and readOnly execute queries in the expected context") {
    val insertCount = dbUtil.writeSession { implicit session =>
      sql"insert into test (id, data) values (1, 'child')".update()
    }
    insertCount should be(1)

    val stored = dbUtil.readOnly { implicit session =>
      sql"select data from test where id = 1".map(_.string("data")).single()
    }
    stored should be(Some("child"))
  }

  test("that writeSession overloads for T and Try[T] both work") {
    assertThrows[RuntimeException] {
      dbUtil.writeSession(_ => 42 / 0)
    }

    val res = dbUtil.writeSession(_ => Failure(RuntimeException("boom")))
    inside(res) { case Failure(ex: RuntimeException) =>
      ex.getMessage should be("boom")
    }
  }

  test("that readOnly overloads for T and Try[T] both work") {
    assertThrows[RuntimeException] {
      dbUtil.readOnly(_ => 42 / 0)
    }

    val res = dbUtil.readOnly(_ => Failure(RuntimeException("boom")))
    inside(res) { case Failure(ex: RuntimeException) =>
      ex.getMessage should be("boom")
    }
  }

  test("buildWhereClause joins conditions and omits empty clauses") {
    dbUtil.writeSession { implicit session =>
      sql"""
            insert into test (id, data) values (1, 'parent1');
            insert into test (id, data) values (2, 'parent2');""".update()
    }

    val whereClause = dbUtil.buildWhereClause(Seq(sqls"id = 1", sqls"data = 'parent1'"))
    val filtered    = dbUtil.readOnly { implicit session =>
      sql"select data from test $whereClause".map(_.string("data")).list()
    }
    filtered should be(List("parent1"))

    val emptyClause = dbUtil.buildWhereClause(Seq.empty)
    val allRows     = dbUtil.readOnly { implicit session =>
      sql"select data from test $emptyClause order by id".map(_.string("data")).list()
    }
    allRows should be(List("parent1", "parent2"))
  }

  test("asRawJsonb and asJsonb bind jsonb values") {
    val rawJson = """{"a":1}"""
    val json    = Map("foo" -> "bar")

    dbUtil.writeSession { implicit session =>
      sql"insert into test_json (id, payload) values (1, ${dbUtil.asRawJsonb(rawJson)})".update()
      sql"insert into test_json (id, payload) values (2, ${dbUtil.asJsonb(json)})".update()
    }

    val rawStored = dbUtil.readOnly { implicit session =>
      sql"select payload::text as payload from test_json where id = 1".map(_.string("payload")).single()
    }
    rawStored should be(Some("{\"a\": 1}"))

    val jsonStored = dbUtil.readOnly { implicit session =>
      sql"select payload::text as payload from test_json where id = 2".map(_.string("payload")).single()
    }
    jsonStored should be(Some("{\"foo\": \"bar\"}"))
  }
}

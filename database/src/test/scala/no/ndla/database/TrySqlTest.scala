/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import no.ndla.database.TrySql.tsql
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import scalikejdbc.*

import scala.util.Failure

class TrySqlTest extends DatabaseIntegrationSuite, UnitSuite, TestEnvironment {
  val dataSource: DataSource = testDataSource.get

  override def beforeAll(): Unit = {
    super.beforeAll()

    dataSource.connectToDatabase()

    DB.autoCommit { case given DBSession =>
      sql"""
            create schema if not exists testschema;
            create table test (id bigserial primary key, data text);
            insert into test (data) values ('example');""".execute()
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    DB.autoCommit { case given DBSession =>
      sql"delete from test".execute()
    }
  }

  test("that execute, update, and updateAndReturnGeneratedKey works") {
    dbUtil.withSession { case given DBSession =>
      tsql"insert into test (data) values ('example')".execute().failIfFailure

      tsql"insert into test (data) values ('example')".update().failIfFailure should be(1)

      val generatedKey = tsql"insert into test (data) values ('example')".updateAndReturnGeneratedKey().failIfFailure
      generatedKey should be > 0L
    }
  }

  test("that runSingle and runSingleTry works") {
    dbUtil.withSession { case given DBSession =>
      tsql"insert into test (data) values ('example')".execute().failIfFailure

      val res1 = tsql"select * from test where data = 'example'".map(_.string("data")).runSingle().failIfFailure
      res1 should be(Some("example"))

      val expectedException = new RuntimeException("No data found!")
      val res2              = tsql"select * from test where data = 'this does not exist'"
        .map(_.string("data"))
        .runSingleTry(expectedException)
      res2 should be(Failure(expectedException))
    }
  }

  test("that runList and runListFlat works") {
    dbUtil.withSession { case given DBSession =>
      tsql"insert into test (data) values ('example')".execute().failIfFailure

      val res1 = tsql"select * from test".map(_.string("data")).runList().failIfFailure
      res1 should be(List("example"))

      val expectedException = new RuntimeException("Something went wrong!")
      val res2              = tsql"select * from test".map(_ => Failure(expectedException)).runListFlat()
      res2 should be(Failure(expectedException))
    }
  }
}

/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import cats.implicits.*
import no.ndla.database.implicits.*
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, UnitTestSuite}
import scalikejdbc.*

import scala.util.Failure

class TrySqlTest extends DatabaseIntegrationSuite, UnitTestSuite, TestEnvironment {
  val dataSource: DataSource = testDataSource.get

  override def beforeAll(): Unit = {
    super.beforeAll()

    dataSource.connectToDatabase()

    DB.autoCommit { implicit session =>
      sql"""
            create schema if not exists testschema;
            create table test (id int primary key, data text);
            create table test2 (id int primary key, data text, parent int not null references test(id));
            create table test3 (id int primary key, data text, parent int not null references test(id));
            create table test4 (id int primary key, data text, parent int not null references test(id));""".execute()
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    DB.autoCommit { implicit session =>
      sql"""
            delete from test4;
            delete from test3;
            delete from test2;
            delete from test;

            insert into test (id, data) values (1, 'parent1');
            insert into test (id, data) values (2, 'parent2');

            insert into test2 (id, data, parent) values (1, 'child1_of_parent1', 1);

            insert into test2 (id, data, parent) values (2, 'child1_of_parent2', 2);
            insert into test2 (id, data, parent) values (3, 'child2_of_parent2', 2);
            insert into test2 (id, data, parent) values (4, 'child3_of_parent2', 2);

            insert into test3 (id, data, parent) values (1, 'child1_of_parent1', 1);

            insert into test4 (id, data, parent) values (1, 'child1_of_parent1', 1);""".execute()
    }
  }

  test("that execute, update, and updateAndReturnGeneratedKey works") {
    dbUtil.withSession { implicit session =>
      tsql"insert into test (id, data) values (42, 'example')".execute().failIfFailure

      tsql"insert into test (id, data) values (43, 'example')".update().failIfFailure should be(1)

      val generatedKey =
        tsql"insert into test (id, data) values (44, 'example')".updateAndReturnGeneratedKey().failIfFailure
      generatedKey should be(44)
    }
  }

  test("that runSingle and runSingleTry works") {
    dbUtil.withSession { implicit session =>
      val res1 = tsql"select * from test where id = 1".map(_.string("data")).runSingle().failIfFailure
      res1 should be(Some("parent1"))

      val expectedException = new RuntimeException("No data found!")
      val res2              = tsql"select * from test where data = 'this does not exist'"
        .map(_.string("data"))
        .runSingleTry(expectedException)
      res2 should be(Failure(expectedException))
    }
  }

  test("that runList and runListFlat works") {
    dbUtil.withSession { implicit session =>
      val res1 = tsql"select * from test where id = 1".map(_.string("data")).runList().failIfFailure
      res1 should be(List("parent1"))

      val expectedException = new RuntimeException("Something went wrong!")
      val res2              = tsql"select * from test".map(_ => Failure(expectedException)).runListFlat()
      res2 should be(Failure(expectedException))
    }
  }

  test("that .one(..).toOne(..) works") {
    dbUtil.withSession { implicit session =>
      val res =
        tsql"select t.data as t_data, t2.data as t2_data from test t inner join test2 t2 on t2.parent = t.id where t.id = 1"
          .one(rs => {
            rs.string("t_data")
          })
          .toOne(_.string("t2_data"))
          .map((parent, child) => parent -> child)
          .runList()
          .failIfFailure

      res should be(List("parent1" -> "child1_of_parent1"))
    }
  }

  test("that .one(..).toMany(..) works") {
    dbUtil.withSession { implicit session =>
      val res =
        tsql"select t.data as t_data, t2.data as t2_data from test t inner join test2 t2 on t2.parent = t.id where t.id = 2"
          .one(_.string("t_data"))
          .toMany(rs => Some(rs.string("t2_data")))
          .map((parent, children) => parent -> children)
          .runList()
          .failIfFailure

      res should be(List("parent2" -> List("child1_of_parent2", "child2_of_parent2", "child3_of_parent2")))
    }
  }

  test("that .one(..).toManies(..) with three arguments works") {
    dbUtil.withSession { implicit session =>
      val res = tsql"""
               select t.data as t_data, t2.data as t2_data, t3.data as t3_data, t4.data as t4_data from test t
               inner join test2 t2 on t2.parent = t.id
               inner join test3 t3 on t3.parent = t.id
               inner join test4 t4 on t4.parent = t.id"""
        .one(_.string("t_data"))
        .toManies(_.string("t2_data").some, _.string("t3_data").some, _.string("t4_data").some)
        .map((parent, children1, children2, children3) => parent -> (children1 ++ children2 ++ children3))
        .runList()
        .failIfFailure

      res should be(List("parent1" -> Seq("child1_of_parent1", "child1_of_parent1", "child1_of_parent1")))
    }
  }
}

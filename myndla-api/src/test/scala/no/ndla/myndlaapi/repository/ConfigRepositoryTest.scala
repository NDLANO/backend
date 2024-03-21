/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.repository

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta}
import no.ndla.myndlaapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome
import scalikejdbc._

import scala.util.{Failure, Success, Try}

class ConfigRepositoryTest
    extends IntegrationSuite(EnablePostgresContainer = true, schemaName = "myndlaapi_test")
    with UnitSuite
    with TestEnvironment {
  override val dataSource: HikariDataSource = testDataSource.get
  override val migrator                     = new DBMigrator

  var repository: ConfigRepository = _

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    postgresContainer match {
      case Failure(ex) =>
        println(s"Postgres container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }
    if (!sys.env.getOrElse("CI", "false").toBoolean) {
      assume(postgresContainer.isSuccess, "Docker environment unavailable for postgres container")
    }
    super.withFixture(test)
  }

  def databaseIsAvailable: Boolean = {
    val res = Try(repository.configCount)
    res.isSuccess
  }

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from configtable;".execute()(session)
      sql"delete from configtable;".execute()(session)
    })
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    DataSource.connectToDatabase()
    migrator.migrate()
  }

  override def beforeEach(): Unit = {
    repository = new ConfigRepository
    if (databaseIsAvailable) {
      emptyTestDatabase
    }
  }

  test("That updating configKey from empty database inserts config") {
    val newConfig = ConfigMeta(
      key = ConfigKey.LearningpathWriteRestricted,
      value = BooleanValue(true),
      updatedAt = NDLADate.fromUnixTime(0),
      updatedBy = "ndlaUser1"
    )

    repository.updateConfigParam(newConfig)

    repository.configCount should be(1)
    repository.getConfigWithKey(ConfigKey.LearningpathWriteRestricted) should be(Success(Some(newConfig)))
  }

  test("That updating config works as expected") {
    val originalConfig = ConfigMeta(
      key = ConfigKey.LearningpathWriteRestricted,
      value = BooleanValue(true),
      updatedAt = NDLADate.fromUnixTime(0),
      updatedBy = "ndlaUser1"
    )

    repository.updateConfigParam(originalConfig)
    repository.configCount should be(1)
    repository.getConfigWithKey(ConfigKey.LearningpathWriteRestricted) should be(Success(Some(originalConfig)))

    val updatedConfig = ConfigMeta(
      key = ConfigKey.LearningpathWriteRestricted,
      value = BooleanValue(false),
      updatedAt = NDLADate.fromUnixTime(10000),
      updatedBy = "ndlaUser2"
    )

    repository.updateConfigParam(updatedConfig)
    repository.configCount should be(1)
    repository.getConfigWithKey(ConfigKey.LearningpathWriteRestricted) should be(Success(Some(updatedConfig)))
  }
}

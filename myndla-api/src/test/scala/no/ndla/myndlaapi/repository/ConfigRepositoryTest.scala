/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.repository

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta}
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.myndlaapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import scalikejdbc.*

import scala.util.Success

class ConfigRepositoryTest extends DatabaseIntegrationSuite with UnitSuite with TestEnvironment {
  override lazy val schemaName: String              = "myndlaapi_test"
  override implicit lazy val dataSource: DataSource = testDataSource.get
  override implicit lazy val migrator: DBMigrator   = new DBMigrator

  var repository: ConfigRepository = scala.compiletime.uninitialized

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from configtable;".execute()(using session)
      sql"delete from configtable;".execute()(using session)
    })
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource.connectToDatabase()
    migrator.migrate()
  }

  override def beforeEach(): Unit = {
    repository = new ConfigRepository
    emptyTestDatabase
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

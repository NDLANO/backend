/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

import java.time.Duration

case class PgContainer(
    PostgresqlVersion: String,
    username: String,
    password: String,
    dbName: String
) extends PostgreSQLContainer(s"postgres:$PostgresqlVersion") {
  this.setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(100)))

  def setPassword(password: String): Unit = this.withPassword(password)
  def setUsername(username: String): Unit = this.withUsername(username)
  def setDatabase(database: String): Unit = this.withDatabaseName(database)

  def configure(username: String, password: String, databaseName: String): Unit = {
    setUsername(username)
    setPassword(password)
    setDatabase(databaseName)
  }

  configure(this.username, this.password, this.dbName)
}

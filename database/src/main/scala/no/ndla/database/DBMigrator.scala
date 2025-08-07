/*
 * Part of NDLA database
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.logging.logTaskTime
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.migration.JavaMigration
import org.flywaydb.core.api.output.MigrateResult

import scala.jdk.CollectionConverters.*

trait DBMigrator {
  this: DataSource & HasDatabaseProps =>
  lazy val migrator: DBMigrator

  case class DBMigrator(migrations: JavaMigration*) extends StrictLogging {
    private def logMigrationResult(result: MigrateResult): Unit = {
      logger.info(s"Num database migrations executed: ${result.migrationsExecuted}")
      val warnings = result.warnings.asScala
      if (warnings.nonEmpty) {
        logger.info(s"With warnings: \n${warnings.mkString("\n")}")
      }
      result.migrations.asScala.foreach { mo =>
        logger.info(
          s"Executed ${mo.`type`} migration: ${mo.category} ${mo.version} '${mo.description}' in ${mo.executionTime} ms"
        )
      }
    }

    def migrate(): Unit = logTaskTime("database migration", logTaskStart = true) {
      DataSource.connectToDatabase()

      val config = Flyway
        .configure()
        .javaMigrations(migrations*)
        .locations(props.MetaMigrationLocation)
        .dataSource(dataSource)
        .schemas(dataSource.getSchema)

      val withTable = props.MetaMigrationTable match {
        case Some(table) => config.table(table)
        case None        => config
      }

      val flyway = withTable.load()

      val migrateResult = flyway.migrate()
      logMigrationResult(migrateResult)
    }
  }
}

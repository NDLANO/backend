/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.db.migrationwithdependencies

import no.ndla.database.TableMigration
import no.ndla.myndlaapi.integration.TaxonomyApiClient
import no.ndla.network.NdlaClient
import scalikejdbc.{DBSession, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}

import java.util.UUID

trait V16__MigrateResourcePaths {
  this: TaxonomyApiClient & NdlaClient =>

  class V16__MigrateResourcePaths extends TableMigration[ResourceRow] {
    override val tableName: String                                 = "resources"
    override lazy val whereClause: scalikejdbc.SQLSyntax           = sqls"path is not null"
    override val chunkSize: Int                                    = 1000
    override def extractRowData(rs: WrappedResultSet): ResourceRow = ResourceRow(
      UUID.fromString(rs.string("id")),
      rs.string("resource_type"),
      rs.string("path")
    )
    override def updateRow(rowData: ResourceRow)(implicit session: DBSession): Int = {
      rowData.resourceType match {
        case "article" | "learningpath" | "multidisciplinary" | "topic" =>
          taxonomyApiClient
            .resolveUrl(rowData.path)
            .map { path => sql"update resources set path=$path where id = ${rowData.id}".update() }
            .get
        case _ => 0
      }
    }
  }
}

case class ResourceRow(id: UUID, resourceType: String, path: String)

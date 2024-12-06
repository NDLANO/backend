/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.db.migrationwithdependencies

import no.ndla.myndlaapi.integration.TaxonomyApiClient
import no.ndla.network.NdlaClient
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import scalikejdbc.{DB, DBSession, scalikejdbcSQLInterpolationImplicitDef}

import java.util.UUID

trait V16__MigrateResourcePaths {
  this: TaxonomyApiClient & NdlaClient =>

  class V16__MigrateResourcePaths extends BaseJavaMigration {
    private val chunkSize = 100;

    override def migrate(context: Context): Unit = DB(context.getConnection)
      .autoClose(false)
      .withinTx { implicit session =>
        migrateResources
      }

    private def migrateResources(implicit session: DBSession): Unit = {
      val count        = countResources.get
      var numPagesLeft = (count / chunkSize) + 1
      var offset       = 0L

      while (numPagesLeft > 0) {
        allResources(offset * chunkSize).foreach { case (id, resourceType, path) =>
          resourceType match {
            case "article" | "learningpath" | "multidisciplinary" | "topic" =>
              val updatedPath = taxonomyApiClient.resolveUrl(path)
              updatedPath.map(updateResource(UUID.fromString(id), _))
            case _ => ()
          }
        }
        numPagesLeft -= 1
        offset += 1
      }
    }

    private def updateResource(id: UUID, path: String)(implicit session: DBSession): Int = {
      sql"update resources set path=$path where id = $id".update()
    }

    private def allResources(offset: Long)(implicit session: DBSession): Seq[(String, String, String)] = {
      sql"select id, resource_type, path from resources order by id limit $chunkSize offset $offset"
        .map(rs => {
          (rs.string("id"), rs.string("resource_type"), rs.string("path"))
        })
        .list()
    }

    private def countResources(implicit session: DBSession): Option[Long] = {
      sql"select count(*) from resources"
        .map(rs => rs.long("count"))
        .single()
    }
  }
}

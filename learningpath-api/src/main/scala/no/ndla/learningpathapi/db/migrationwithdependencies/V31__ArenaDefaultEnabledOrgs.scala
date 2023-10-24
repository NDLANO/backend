/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.db.migrationwithdependencies

import io.circe.Json
import io.circe.syntax._
import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.LearningpathApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V31__ArenaDefaultEnabledOrgs(properties: LearningpathApiProperties) extends BaseJavaMigration {

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      insertConfig
    }

  def insertConfig(implicit session: DBSession): Unit = {
    val document = Json.obj(
      "key"       -> Json.fromString("MY_NDLA_ENABLED_ORGS"),
      "value"     -> Json.obj("value" -> Json.fromValues(orgs.map(Json.fromString))),
      "updatedAt" -> NDLADate.now().asJson,
      "updatedBy" -> Json.fromString("System")
    )

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document.noSpaces)

    val inserted = sql"""
         INSERT INTO configtable(configkey, value)
         VALUES (
             'MY_NDLA_ENABLED_ORGS',
             $dataObject
         )
         """.update.apply()

    if (inserted != 1) throw new RuntimeException("Failed to insert MY_NDLA_ENABLED_ORGS")
  }

  private def orgs: List[String] = properties.Environment match {
    case "local" | "test" =>
      List(
        "Agder fylkeskommune",
        "Nordland fylkeskommune",
        "Rogaland fylkeskommune",
        "Universitetet i Rogn"
      )
    case _ =>
      List(
        "Agder fylkeskommune",
        "Nordland fylkeskommune",
        "Rogaland fylkeskommune",
        "Innlandet fylkeskommune",
        "Møre og Romsdal fylkeskommune",
        "Troms og Finnmark fylkeskommune",
        "Trøndelag fylkeskommune",
        "Vestfold og Telemark fylkeskommune",
        "Vestland fylkeskommune",
        "Viken fylkeskommune",
        "Universitetet i Rogn"
      )
  }

}

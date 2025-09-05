/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.repository

import com.typesafe.scalalogging.StrictLogging
import io.circe.syntax.*
import no.ndla.frontpageapi.model.domain.{DBFrontPage, FrontPage}
import org.postgresql.util.PGobject
import scalikejdbc.*
import cats.implicits.*

import scala.util.Try

class FrontPageRepository(using dBFrontPage: DBFrontPage) extends StrictLogging {
  import FrontPage._

  def newFrontPage(page: FrontPage)(implicit session: DBSession = AutoSession): Try[FrontPage] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(page.asJson.noSpacesDropNull)

    Try(
      sql"insert into ${dBFrontPage.DBFrontPageData.table} (document) values ($dataObject)"
        .updateAndReturnGeneratedKey()
    ).map(deleteAllBut).map(_ => page)
  }

  private def deleteAllBut(id: Long)(implicit session: DBSession): Try[Long] = {
    Try(
      sql"delete from ${dBFrontPage.DBFrontPageData.table} where id<>${id} "
        .update()
    ).map(_ => id)
  }

  def getFrontPage(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[FrontPage]] = Try {
    val fr = dBFrontPage.DBFrontPageData.syntax("fr")
    sql"select ${fr.result.*} from ${dBFrontPage.DBFrontPageData.as(fr)} order by fr.id desc limit 1"
      .map(dBFrontPage.DBFrontPageData.fromResultSet(fr))
      .single()
      .sequence
  }.flatten

}

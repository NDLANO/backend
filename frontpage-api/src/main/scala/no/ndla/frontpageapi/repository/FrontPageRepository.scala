/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.repository

import io.circe.syntax._
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.domain.FrontPageData
import org.postgresql.util.PGobject
import scalikejdbc._
import FrontPageData._

import scala.util.{Failure, Success, Try}

trait FrontPageRepository {
  this: DataSource =>
  val frontPageRepository: FrontPageRepository

  class FrontPageRepository {

    def newFrontPage(page: FrontPageData)(implicit session: DBSession = AutoSession): Try[FrontPageData] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(page.asJson.noSpacesDropNull)

      Try(
        sql"insert into ${FrontPageData.table} (document) values (${dataObject})"
          .updateAndReturnGeneratedKey()
      ).map(deleteAllBut).map(_ => page)
    }

    private def deleteAllBut(id: Long)(implicit session: DBSession = AutoSession) = {
      Try(
        sql"delete from ${FrontPageData.table} where id<>${id} "
          .update()
      ).map(_ => id)
    }

    def get(implicit session: DBSession = ReadOnlyAutoSession): Option[FrontPageData] = {
      val fr = FrontPageData.syntax("fr")

      Try(
        sql"select ${fr.result.*} from ${FrontPageData.as(fr)} order by fr.id desc limit 1"
          .map(FrontPageData.fromDb(fr))
          .single()
      ) match {
        case Success(Some(Success(s))) => Some(s)
        case Success(Some(Failure(ex))) =>
          ex.printStackTrace()
          None
        case Success(None) => None
        case Failure(ex) =>
          ex.printStackTrace()
          None
      }
    }

  }
}

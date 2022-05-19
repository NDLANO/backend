/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.repository

import io.circe.syntax._
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.domain.{DBFrontPageData, FrontPageData}
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait FrontPageRepository {
  this: DataSource with DBFrontPageData =>
  val frontPageRepository: FrontPageRepository

  class FrontPageRepository {
    import FrontPageData._

    def newFrontPage(page: FrontPageData)(implicit session: DBSession = AutoSession): Try[FrontPageData] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(page.asJson.noSpacesDropNull)

      Try(
        sql"insert into ${DBFrontPageData.table} (document) values (${dataObject})"
          .updateAndReturnGeneratedKey()
      ).map(deleteAllBut).map(_ => page)
    }

    private def deleteAllBut(id: Long)(implicit session: DBSession) = {
      Try(
        sql"delete from ${DBFrontPageData.table} where id<>${id} "
          .update()
      ).map(_ => id)
    }

    def get(implicit session: DBSession = ReadOnlyAutoSession): Option[FrontPageData] = {
      val fr = DBFrontPageData.syntax("fr")

      Try(
        sql"select ${fr.result.*} from ${DBFrontPageData.as(fr)} order by fr.id desc limit 1"
          .map(DBFrontPageData.fromDb(fr))
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

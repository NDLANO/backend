/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.repository

import io.circe.syntax._
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPageData, FilmFrontPageData}
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait FilmFrontPageRepository {
  this: DataSource with DBFilmFrontPageData =>
  val filmFrontPageRepository: FilmFrontPageRepository

  class FilmFrontPageRepository {
    import FilmFrontPageData._

    def newFilmFrontPage(page: FilmFrontPageData)(implicit session: DBSession = AutoSession): Try[FilmFrontPageData] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(page.asJson.noSpacesDropNull)

      Try(
        sql"insert into ${DBFilmFrontPageData.table} (document) values (${dataObject})"
          .updateAndReturnGeneratedKey()
      ).map(deleteAllBut).map(_ => page)
    }

    private def deleteAllBut(id: Long)(implicit session: DBSession) = {
      Try(
        sql"delete from ${DBFilmFrontPageData.table} where id<>${id} "
          .update()
      ).map(_ => id)
    }

    def get(implicit session: DBSession = ReadOnlyAutoSession): Option[FilmFrontPageData] = {
      val fr = DBFilmFrontPageData.syntax("fr")

      Try(
        sql"select ${fr.result.*} from ${DBFilmFrontPageData.as(fr)} order by fr.id desc limit 1"
          .map(DBFilmFrontPageData.fromDb(fr))
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

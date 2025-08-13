/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.repository

import io.circe.syntax.*
import no.ndla.database.DataSource
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPage, FilmFrontPage}
import org.log4s.Logger
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

trait FilmFrontPageRepository {
  this: DataSource & DBFilmFrontPage =>
  val filmFrontPageRepository: FilmFrontPageRepository

  class FilmFrontPageRepository {
    val logger: Logger = org.log4s.getLogger
    import FilmFrontPage._

    def newFilmFrontPage(page: FilmFrontPage)(implicit session: DBSession = AutoSession): Try[FilmFrontPage] = {
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

    def get(implicit session: DBSession = ReadOnlyAutoSession): Option[FilmFrontPage] = {
      val fr = DBFilmFrontPageData.syntax("fr")

      Try(
        sql"select ${fr.result.*} from ${DBFilmFrontPageData.as(fr)} order by fr.id desc limit 1"
          .map(DBFilmFrontPageData.fromDb(fr))
          .single()
      ) match {
        case Success(Some(Success(s)))  => Some(s)
        case Success(Some(Failure(ex))) =>
          logger.error(ex)("Error while decoding film front page")
          None
        case Success(None) => None
        case Failure(ex)   =>
          logger.error(ex)("Error while getting film front page from database")
          None
      }

    }

    def update(page: FilmFrontPage)(implicit session: DBSession = AutoSession): Try[FilmFrontPage] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(page.asJson.noSpacesDropNull)

      Try(
        sql"update ${DBFilmFrontPageData.table} set document=$dataObject".update()
      ).map(_ => page)
    }

  }

}

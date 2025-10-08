/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.repository

import com.typesafe.scalalogging.StrictLogging
import io.circe.syntax.*
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPage, FilmFrontPage}
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

class FilmFrontPageRepository(using dBFilmFrontPage: DBFilmFrontPage) extends StrictLogging {
  import FilmFrontPage._

  def newFilmFrontPage(page: FilmFrontPage)(implicit session: DBSession = AutoSession): Try[FilmFrontPage] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(page.asJson.noSpacesDropNull)

    Try(
      sql"insert into ${dBFilmFrontPage.DBFilmFrontPageData.table} (document) values (${dataObject})"
        .updateAndReturnGeneratedKey()
    ).map(deleteAllBut).map(_ => page)
  }

  private def deleteAllBut(id: Long)(implicit session: DBSession) = {
    Try(
      sql"delete from ${dBFilmFrontPage.DBFilmFrontPageData.table} where id<>${id} "
        .update()
    ).map(_ => id)
  }

  def get(implicit session: DBSession = ReadOnlyAutoSession): Option[FilmFrontPage] = {
    val fr = dBFilmFrontPage.DBFilmFrontPageData.syntax("fr")

    Try(
      sql"select ${fr.result.*} from ${dBFilmFrontPage.DBFilmFrontPageData.as(fr)} order by fr.id desc limit 1"
        .map(dBFilmFrontPage.DBFilmFrontPageData.fromDb(fr))
        .single()
    ) match {
      case Success(Some(Success(s)))  => Some(s)
      case Success(Some(Failure(ex))) =>
        logger.error("Error while decoding film front page", ex)
        None
      case Success(None) => None
      case Failure(ex)   =>
        logger.error("Error while getting film front page from database", ex)
        None
    }

  }

  def update(page: FilmFrontPage)(implicit session: DBSession = AutoSession): Try[FilmFrontPage] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(page.asJson.noSpacesDropNull)

    Try(
      sql"update ${dBFilmFrontPage.DBFilmFrontPageData.table} set document=$dataObject".update()
    ).map(_ => page)
  }

}

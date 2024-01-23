/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe.parser._
import no.ndla.frontpageapi.Props
import scalikejdbc.WrappedResultSet
import scalikejdbc._
import cats.implicits._

import scala.util.Try

case class Menu(articleId: Long, menu: List[Menu], hideLevelFlag: Boolean)

case class FrontPage(
    articleId: Long,
    menu: List[Menu]
)

object FrontPage {
  implicit val encoder: Encoder[FrontPage] = deriveEncoder[FrontPage]

  private[domain] def decodeJson(document: String): Try[FrontPage] = {
    parse(document).flatMap(_.as[FrontPage]).toTry
  }
}

trait DBFrontPageData {
  this: Props =>

  object DBFrontPageData extends SQLSyntaxSupport[FrontPage] {
    override val tableName                  = "mainfrontpage"
    override val schemaName: Option[String] = props.MetaSchema.some

    def fromResultSet(lp: SyntaxProvider[FrontPage])(rs: WrappedResultSet): Try[FrontPage] =
      fromResultSet(lp.resultName)(rs)

    private def fromResultSet(lp: ResultName[FrontPage])(rs: WrappedResultSet): Try[FrontPage] = {
      val document = rs.string(lp.c("document"))
      FrontPage.decodeJson(document)
    }

  }
}

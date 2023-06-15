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

case class Menu(articleId: Long, menu: List[Menu])

case class FrontPageData(
    articleId: Long,
    menu: List[Menu]
)

object FrontPageData {
  implicit val encoder: Encoder[FrontPageData] = deriveEncoder[FrontPageData]

  private[domain] def decodeJson(document: String): Try[FrontPageData] = {
    parse(document).flatMap(_.as[FrontPageData]).toTry
  }
}

trait DBFrontPageData {
  this: Props =>

  object DBFrontPageData extends SQLSyntaxSupport[FrontPageData] {
    override val tableName                  = "mainfrontpage"
    override val schemaName: Option[String] = props.MetaSchema.some

    def fromResultSet(lp: SyntaxProvider[FrontPageData])(rs: WrappedResultSet): Try[FrontPageData] =
      fromResultSet(lp.resultName)(rs)

    private def fromResultSet(lp: ResultName[FrontPageData])(rs: WrappedResultSet): Try[FrontPageData] = {
      val document = rs.string(lp.c("document"))
      FrontPageData.decodeJson(document)
    }

  }
}

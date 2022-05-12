/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe.parser._
import no.ndla.frontpageapi.Props
import scalikejdbc.WrappedResultSet
import scalikejdbc._
import cats.implicits._
import io.circe.Encoder

import scala.util.Try

case class FrontPageData(topical: List[String], categories: List[SubjectCollection])

object FrontPageData {
  implicit val encoder: Encoder[FrontPageData] = deriveEncoder

  private[domain] def decodeJson(json: String): Try[FrontPageData] = {
    parse(json).flatMap(_.as[FrontPageData]).toTry
  }
}

trait DBFrontPageData {
  this: Props =>

  object DBFrontPageData extends SQLSyntaxSupport[FrontPageData] {
    override val tableName                  = "mainfrontpage"
    override val schemaName: Option[String] = props.MetaSchema.some

    def fromDb(lp: SyntaxProvider[FrontPageData])(rs: WrappedResultSet): Try[FrontPageData] =
      fromDb(lp.resultName)(rs)

    private def fromDb(lp: ResultName[FrontPageData])(rs: WrappedResultSet): Try[FrontPageData] = {
      val document = rs.string(lp.c("document"))
      FrontPageData.decodeJson(document)
    }

  }
}

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
import no.ndla.frontpageapi.FrontpageApiProperties
import scalikejdbc.WrappedResultSet
import scalikejdbc._
import cats.implicits._
import io.circe.Encoder

import scala.util.Try

case class FrontPageData(topical: List[String], categories: List[SubjectCollection])

object FrontPageData extends SQLSyntaxSupport[FrontPageData] {
  override val tableName = "mainfrontpage"
  override val schemaName = FrontpageApiProperties.MetaSchema.some

  private[domain] def decodeJson(json: String): Try[FrontPageData] = {
    parse(json).flatMap(_.as[FrontPageData]).toTry
  }

  implicit val encoder: Encoder[FrontPageData] = deriveEncoder

  def fromDb(lp: SyntaxProvider[FrontPageData])(rs: WrappedResultSet): Try[FrontPageData] =
    fromDb(lp.resultName)(rs)

  private def fromDb(lp: ResultName[FrontPageData])(rs: WrappedResultSet): Try[FrontPageData] = {
    val document = rs.string(lp.c("document"))
    decodeJson(document)
  }

}

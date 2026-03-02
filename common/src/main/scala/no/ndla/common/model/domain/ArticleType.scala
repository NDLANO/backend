/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*
import no.ndla.common.errors.ValidationException

sealed abstract class ArticleType(override val entryName: String) extends EnumEntry

object ArticleType extends Enum[ArticleType] with CirceEnum[ArticleType] {
  case object Standard         extends ArticleType("standard")
  case object TopicArticle     extends ArticleType("topic-article")
  case object FrontpageArticle extends ArticleType("frontpage-article")
  case object CaseArticle      extends ArticleType("case-article")

  implicit def schema: Schema[ArticleType]    = schemaForEnumEntry[ArticleType]
  implicit def codec: PlainCodec[ArticleType] = plainCodecEnumEntry[ArticleType]

  val values: IndexedSeq[ArticleType] = findValues

  def all: Seq[String]                        = ArticleType.values.map(_.entryName)
  def valueOf(s: String): Option[ArticleType] = ArticleType.withNameOption(s)

  def valueOfOrError(s: String): ArticleType = valueOf(s).getOrElse(
    throw ValidationException(
      "articleType",
      s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}.",
    )
  )
}

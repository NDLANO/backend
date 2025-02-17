/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import scala.util.{Failure, Try}

object ContentURIUtil {
  case class NotUrnPatternException(message: String) extends RuntimeException(message)

  private val ArticlePattern   = """(urn:)?(article:)?(\d*)#?(\d*)""".r
  private val FrontpagePattern = """urn:frontpage:(\d*)""".r

  def parseFrontpageId(idString: String): Try[Long] = {
    idString match {
      case FrontpagePattern(id) =>
        Try(id.toLong)
      case _ =>
        Failure(
          NotUrnPatternException(s"Pattern \"$idString\" passed to `parseFrontpageId` did not match urn pattern.")
        )
    }

  }

  type Result = (Try[Long], Option[Int])
  def parseArticleIdAndRevision(idString: String): Result = {
    idString match {
      case ArticlePattern(_, _, id, rev) =>
        (
          Try(id.toLong),
          Try(rev.toInt).toOption
        )
      case _ =>
        Failure(
          NotUrnPatternException(
            s"Pattern \"$idString\" passed to `parseArticleIdAndRevision` did not match urn pattern."
          )
        ) -> None
    }
  }
}

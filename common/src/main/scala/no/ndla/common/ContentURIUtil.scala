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

  private val Pattern = """(urn:)?(article:)?(\d*)#?(\d*)""".r
  type Result = (Try[Long], Option[Int])
  def parseArticleIdAndRevision(idString: String): Result = {
    idString match {
      case Pattern(_, _, id, rev) =>
        (
          Try(id.toLong),
          Try(rev.toInt).toOption
        )
      case _ =>
        Failure(
          NotUrnPatternException("Pattern passed to `parseArticleIdAndRevision` did not match urn pattern.")
        ) -> None
    }
  }
}

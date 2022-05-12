package no.ndla.common

import scala.util.Try

object ContentURIUtil {

  private val Pattern = """(urn:)?(article:)?(\d*)#?(\d*)""".r
  def parseArticleIdAndRevision(idString: String): (Try[Long], Option[Int]) = {
    idString match {
      case Pattern(_, _, id, rev) =>
        (
          Try(id.toLong),
          Try(rev.toInt).toOption
        )
    }
  }
}

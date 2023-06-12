package no.ndla.common

object StringUtil {
  def emptySomeToNone(s: Option[String]): Option[String] = s.filter(_.nonEmpty)
}

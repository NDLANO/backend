package no.ndla.audioapi.model

package object domain {

  def emptySomeToNone(lang: Option[String]): Option[String] = lang.filter(_.nonEmpty)

}

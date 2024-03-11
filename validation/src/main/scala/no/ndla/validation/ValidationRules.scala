package no.ndla.validation
import enumeratum.Json4s
import no.ndla.validation.model.{HtmlRulesFile, MathMLRulesFile}
import org.json4s._
import org.json4s.native.JsonMethods.parse

import scala.io.Source

object ValidationRules {

  def embedTagRulesJson: HtmlRulesFile = {
    implicit val formats: Formats = org.json4s.DefaultFormats + Json4s.serializer(TagAttribute)
    val classLoader               = getClass.getClassLoader
    parse(Source.fromResource("embed-tag-rules.json", classLoader).mkString).extract[HtmlRulesFile]
  }
  def htmlRulesJson: HtmlRulesFile = {
    implicit val formats: Formats = org.json4s.DefaultFormats + Json4s.serializer(TagAttribute)
    val classLoader               = getClass.getClassLoader
    parse(Source.fromResource("html-rules.json", classLoader).mkString).extract[HtmlRulesFile]
  }
  def mathMLRulesJson: MathMLRulesFile = {
    implicit val formats: Formats = org.json4s.DefaultFormats + Json4s.serializer(TagAttribute)
    val classLoader               = getClass.getClassLoader
    parse(Source.fromResource("mathml-rules.json", classLoader).mkString).extract[MathMLRulesFile]
  }
}

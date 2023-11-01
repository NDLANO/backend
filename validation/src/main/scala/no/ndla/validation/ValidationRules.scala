package no.ndla.validation
import enumeratum.Json4s
import no.ndla.validation.model.{HtmlRulesFile, MathMLRulesFile}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.JsonMethods.parse

import scala.io.Source

object ValidationRules {

  def embedTagRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("embed-tag-rules.json").mkString)
  def htmlRulesJson: HtmlRulesFile = {
    implicit val formats: Formats = org.json4s.DefaultFormats + Json4s.serializer(TagAttribute)
    parse(Source.fromResource("html-rules.json").mkString).extract[HtmlRulesFile]
  }
  def mathMLRulesJson: MathMLRulesFile = {
    implicit val formats: Formats = org.json4s.DefaultFormats + Json4s.serializer(TagAttribute)
    parse(Source.fromResource("mathml-rules.json").mkString).extract[MathMLRulesFile]
  }

  private def convertJsonStr(jsonStr: String): Map[String, Any] = {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }

}

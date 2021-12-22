package no.ndla.validation
import org.json4s.native.JsonMethods.parse

import scala.io.Source

object ValidationRules {

  def htmlRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("html-rules.json").mkString)
  def embedTagRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("embed-tag-rules.json").mkString)
  def mathMLRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("mathml-rules.json").mkString)

  private def convertJsonStr(jsonStr: String): Map[String, Any] = {
    implicit val formats = org.json4s.DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }

}

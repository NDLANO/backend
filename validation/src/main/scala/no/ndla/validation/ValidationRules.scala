/*
 * Part of NDLA validation
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.validation

import no.ndla.common.CirceUtil
import no.ndla.validation.model.{HtmlRulesFile, MathMLRulesFile}

import scala.io.Source

object ValidationRules {

  def embedTagRulesJson: HtmlRulesFile = {
    val classLoader = getClass.getClassLoader
    val jsonStr     = Source.fromResource("embed-tag-rules.json", classLoader).mkString
    val parsed      = CirceUtil.tryParseAs[HtmlRulesFile](jsonStr)
    parsed.get
  }
  def htmlRulesJson: HtmlRulesFile = {
    val classLoader = getClass.getClassLoader
    val jsonStr     = Source.fromResource("html-rules.json", classLoader).mkString
    val parsed      = CirceUtil.tryParseAs[HtmlRulesFile](jsonStr)
    parsed.get
  }
  def mathMLRulesJson: MathMLRulesFile = {
    val classLoader = getClass.getClassLoader
    val jsonStr     = Source.fromResource("mathml-rules.json", classLoader).mkString
    val parsed      = CirceUtil.tryParseAs[MathMLRulesFile](jsonStr)
    parsed.get
  }
}

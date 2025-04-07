/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import no.ndla.common.configuration.HasBaseProps
import no.ndla.testbase.UnitTestSuiteBase

import scala.util.Properties.{propOrNone, setProp}

trait UnitTestSuite extends UnitTestSuiteBase with HasBaseProps {
  setPropEnv("DISABLE_LICENSE", "true"): Unit

  def setPropEnv(key: String, value: String): String = {
    props.propFromTestValue(key, value): Unit
    setProp(key, value)
  }

  def getPropEnv(key: String): Option[String] = {
    propOrNone(key)
  }

  def getPropEnvs(keys: String*): Map[String, String] = getPropEnvsFromSeq(keys)

  def getPropEnvsFromSeq(keys: Seq[String]): Map[String, String] = {
    keys.flatMap(key => getPropEnv(key).map(value => key -> value)).toMap
  }
}

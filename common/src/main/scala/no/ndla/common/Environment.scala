/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import no.ndla.common.configuration.EnvironmentNotFoundException

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Properties.propOrElse
import scala.util.Properties.propOrNone

/** Contains some helpers to setup and fetch props from the SystemProperties */
object Environment {

  /** UNSAFE: Will throw [[EnvironmentNotFoundException]] if property is not found */
  def unsafeProp(key: String): String = propOrElse(key, throw EnvironmentNotFoundException.singleKey(key))

  def booleanPropOrFalse(key: String): Boolean = {
    propOrNone(key).flatMap(_.toBooleanOption).getOrElse(false)
  }

  def setPropsFromEnv(): Unit = {
    val envMap = System.getenv()
    envMap
      .asScala
      .foreach { case (key, value) =>
        System.setProperty(key, value)
      }
  }
}

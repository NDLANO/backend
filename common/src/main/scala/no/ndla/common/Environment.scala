/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Properties.propOrElse
import scala.util.Properties.propOrNone

/** Contains some helpers to setup and fetch props from the SystemProperties */
object Environment {

  case class EnvironmentNotFoundException(key: String) extends RuntimeException(s"Unable to load property $key")

  /** UNSAFE: Will throw [[EnvironmentNotFoundException]] if property is not found */
  def prop(key: String): String = propOrElse(key, throw EnvironmentNotFoundException(key))

  def booleanPropOrFalse(key: String): Boolean = {
    propOrNone(key).flatMap(_.toBooleanOption).getOrElse(false)
  }

  def setPropsFromEnv(): Unit = {
    val envMap = System.getenv()
    envMap.asScala.foreach { case (key, value) =>
      System.setProperty(key, value)
    }
  }
}

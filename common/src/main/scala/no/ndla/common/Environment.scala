/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import com.amazonaws.regions.Regions

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Properties.propOrElse
import scala.util.Properties.propOrNone

/** Contains some helpers to setup and fetch props from the SystemProperties */
object Environment {

  case class EnvironmentNotFoundException(key: String) extends RuntimeException(s"Unable to load property $key")

  /** UNSAFE: Will throw [[EnvironmentNotFoundException]] if property is not found */
  def prop(key: String): String = propOrElse(key, throw EnvironmentNotFoundException(key))

  /** Will try to derive aws-region from ec2 instance metadata if `auto` is specified in the property If another string
    * is passed, it will be attempted to be parsed as a aws region. Otherwise the default region will be used.
    */
  def propToAwsRegion(key: String, defaultRegion: Regions = Regions.EU_WEST_1): Regions = {
    val specifiedRegion = propOrNone(key)
    specifiedRegion
      .flatMap {
        case "auto" => Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))
        case str    => Option(Regions.fromName(str))
      }
      .getOrElse(defaultRegion)
  }

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

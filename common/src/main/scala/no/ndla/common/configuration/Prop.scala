/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

case class Prop(
    name: String,
    var value: Option[String],
    var failure: Option[Throwable],
    var defaultValue: Boolean
) {
  def unsafeGet: String = value match {
    case Some(v) => v
    case None    => throw EnvironmentNotFoundException.singleKey(name)
  }

  def setValue(newValue: String): Unit = {
    this.failure = None
    this.defaultValue = false
    this.value = Some(newValue)
  }

  def successful: Boolean = failure.isEmpty

  override def toString: String = {
    value match {
      case Some(value) => value
      case None        => throw EnvironmentNotFoundException.singleKey(name)
    }
  }

}

object Prop {
  implicit def propToString(prop: Prop): String = prop.toString

  def failed(key: String): Prop = {
    Prop(key, None, Some(EnvironmentNotFoundException.singleKey(key)), defaultValue = false)
  }

  /** Do not call this from production code it is unsafe, only tests please */
  def propFromTestValue(value: String): Prop = {
    Prop("<UNKNOWN_KEY>", Some(value), None, defaultValue = false)
  }

}

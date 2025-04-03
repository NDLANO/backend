/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

case class Prop[T](
    name: String,
    var value: Option[T],
    var failure: Option[Throwable],
    var defaultValue: Boolean
) {
  def unsafeGet: T = value match {
    case Some(v) => v
    case None    => throw EnvironmentNotFoundException.singleKey(name)
  }

  def setValue(newValue: T): Unit = {
    this.failure = None
    this.defaultValue = false
    this.value = Some(newValue)
  }

  def successful: Boolean = failure.isEmpty

  def flatMap[R](f: T => Option[R]): Prop[R] = {
    val newValue = value.flatMap(f)

    val newFailure = (failure, newValue) match {
      case (Some(ex), _)   => Some(ex)
      case (None, Some(_)) => None
      case (None, None)    => Some(EnvironmentNotFoundException(s"No value found after flatMap: $name"))
    }

    Prop[R](name, newValue, newFailure, defaultValue)
  }

  override def toString: String = {
    value match {
      case Some(value) => value.toString
      case None =>
        throw EnvironmentNotFoundException.singleKey(name)
    }
  }

}

object Prop {
  implicit def propToString(prop: Prop[?]): String = prop.toString

  def failed[T](key: String): Prop[T] = {
    Prop[T](key, None, Some(EnvironmentNotFoundException.singleKey(key)), defaultValue = false)
  }

  /** Do not call this from production code it is unsafe, only tests please */
  def propFromTestValue[T](value: T): Prop[T] = {
    Prop[T]("<UNKNOWN_KEY>", Some(value), None, defaultValue = false)
  }

}

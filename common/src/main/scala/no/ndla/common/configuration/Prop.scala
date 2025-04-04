/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

case class Prop(var reference: PropValue) {
  def key: String = reference.key

  def unsafeGet: String = reference match {
    case LoadedProp(_, value)   => value
    case FailedProp(_, failure) => throw failure
  }

  def successful: Boolean = reference match {
    case _: LoadedProp => true
    case _: FailedProp => false
  }

  def setValue(newValue: String): Unit = {
    reference = LoadedProp(key, newValue)
  }

  override def toString: String = {
    reference match {
      case x: LoadedProp => x.value
      case _             => throw EnvironmentNotFoundException.singleKey(key)
    }
  }
}

sealed trait PropValue {
  val key: String
}

case class LoadedProp(
    key: String,
    value: String
) extends PropValue

case class FailedProp(
    key: String,
    failure: Throwable
) extends PropValue

object Prop {
  implicit def propToString(prop: Prop): String = prop.toString

  def failed(key: String): Prop = {
    Prop(FailedProp(key, EnvironmentNotFoundException.singleKey(key)))
  }
  def successful(key: String, value: String): Prop = {
    Prop(LoadedProp(key, value))
  }
}

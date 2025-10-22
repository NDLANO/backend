/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

case class Prop[T](var reference: PropValue[T]) {
  def key: String = reference.key

  def unsafeGet: T = reference match {
    case LoadedProp(_, value)   => value
    case FailedProp(_, failure) => throw failure
  }

  def successful: Boolean = reference match {
    case _: LoadedProp[T] => true
    case _: FailedProp[T] => false
  }

  def setValue(newValue: T): Unit                    = setReference(LoadedProp[T](key, newValue))
  def setReference(newReference: PropValue[T]): Unit = reference = newReference

  override def toString: String = {
    reference match {
      case x: LoadedProp[T] => x.value.toString
      case _                => throw EnvironmentNotFoundException.singleKey(key)
    }
  }
}

sealed trait PropValue[T] {
  val key: String
}

case class LoadedProp[T](key: String, value: T) extends PropValue[T]

case class FailedProp[T](key: String, failure: Throwable) extends PropValue[T]

object Prop {
  implicit def propToString(prop: Prop[?]): String = prop.toString

  def failed[T](key: String): Prop[T] = {
    Prop(FailedProp(key, EnvironmentNotFoundException.singleKey(key)))
  }
  def successful[T](key: String, value: T): Prop[T] = {
    Prop(LoadedProp(key, value))
  }
}

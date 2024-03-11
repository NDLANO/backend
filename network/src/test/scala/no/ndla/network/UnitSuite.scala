/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.util.Properties.{setProp, propOrNone, clearProp}

abstract class UnitSuite
    extends AnyFunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  def withEnv(key: String, value: Option[String])(toDoWithEnv: => Any): String = {
    val originalEnv = propOrNone(key)

    value match {
      case Some(envValue) => setProp(key, envValue)
      case None           => clearProp(key)
    }

    toDoWithEnv

    originalEnv match {
      case Some(original) => setProp(key, original)
      case None           => clearProp(key)
    }
  }
}

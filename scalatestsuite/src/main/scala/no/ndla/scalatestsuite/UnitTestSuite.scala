/*
 * Part of NDLA scalatestsuite.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.scalatestsuite

import org.joda.time.{DateTime, DateTimeUtils}
import org.mockito.scalatest.MockitoSugar
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.util.Properties.{setProp, propOrNone, envOrNone}

abstract class UnitTestSuite
    extends AnyFunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  def setEnv(key: String, value: String): String = env.put(key, value)

  def setPropEnv(key: String, value: String): String = {
    setEnv(key, value)
    setProp(key, value)
  }

  def setPropEnv(map: Map[String, String]): Unit = {
    map.map { case (key, value) => setPropEnv(key, value) }
  }

  def setPropEnv(keyValueTuples: (String, String)*): Unit = setPropEnv(keyValueTuples.toMap)

  def getPropEnv(key: String): Option[String] = {
    propOrNone(key).orElse(envOrNone(key))
  }

  def getPropEnvs(keys: String*): Map[String, String] = getPropEnvsFromSeq(keys)

  def getPropEnvsFromSeq(keys: Seq[String]): Map[String, String] = {
    keys.flatMap(key => getPropEnv(key).map(value => key -> value)).toMap
  }

  def setEnvIfAbsent(key: String, value: String): String =
    env.putIfAbsent(key, value)

  private def env = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    field
      .get(System.getenv())
      .asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
  }

  def withFrozenTime(time: DateTime = new DateTime())(toExecute: => Any): Unit = {
    DateTimeUtils.setCurrentMillisFixed(time.getMillis)
    toExecute
    DateTimeUtils.setCurrentMillisSystem()
  }
}

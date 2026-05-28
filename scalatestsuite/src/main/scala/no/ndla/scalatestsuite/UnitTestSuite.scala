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
import scala.util.{Failure, Success, Try}

trait UnitTestSuite extends UnitTestSuiteBase with HasBaseProps {
  setPropEnv("DISABLE_LICENSE", "true"): Unit
  setPropEnv("FEIDE_CLIENT_ID", "feide-client-id"): Unit

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

  def blockUntilHealthy(endpoint: String): Unit = {
    blockUntilSuccess(() => {
      import sttp.client4.quick.*
      val req = quickRequest.get(uri"$endpoint")
      Try(req.send()) match {
        case Failure(exception)                         => Failure(exception)
        case Success(result) if result.code.code != 200 =>
          Failure(new RuntimeException(s"Healthcheck failed with status ${result.code.code} and body ${result.body}"))
        case Success(_) => Success(())
      }
    })
  }

}

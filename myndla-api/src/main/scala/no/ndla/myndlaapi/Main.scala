/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.errors.ExceptionLogHandler

object Main {
  def main(args: Array[String]): Unit = ExceptionLogHandler.default {
    setPropsFromEnv()
    val props     = new MyNdlaApiProperties()
    val mainClass = new MainClass(props)
    mainClass.run()
  }
}

/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import no.ndla.common.Environment.setPropsFromEnv

object Main {
  def main(args: Array[String]): Unit = {
    setPropsFromEnv()
    val props     = new FrontpageApiProperties
    val mainClass = new MainClass(props)
    mainClass.run()
  }
}

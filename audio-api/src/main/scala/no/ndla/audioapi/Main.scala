/*
 * Part of NDLA audio-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.common.Environment.setPropsFromEnv

object Main {
  def main(args: Array[String]): Unit = {
    setPropsFromEnv()
    val props     = new AudioApiProperties
    val mainClass = new MainClass(props)
    mainClass.run()
  }
}

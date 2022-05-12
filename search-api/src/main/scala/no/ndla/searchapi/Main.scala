/*
 * Part of NDLA search-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.common.Environment.setPropsFromEnv

object Main {
  def main(args: Array[String]): Unit = {
    setPropsFromEnv()
    val props     = new SearchApiProperties
    val mainClass = new MainClass(props)
    mainClass.start()
  }
}

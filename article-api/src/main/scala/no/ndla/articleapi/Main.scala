/*
 * Part of NDLA article-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.common.Environment.setPropsFromEnv

object Main {
  def main(args: Array[String]): Unit = {
    setPropsFromEnv()
    val props     = new ArticleApiProperties
    val mainClass = new MainClass(props)
    mainClass.run()
  }
}

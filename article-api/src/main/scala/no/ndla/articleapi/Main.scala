/*
 * Part of NDLA article-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.errors.ExceptionLogHandler

object Main {
  def main(args: Array[String]): Unit = ExceptionLogHandler.default {
    setPropsFromEnv()
    val props     = new ArticleApiProperties
    val mainClass = new MainClass(props)
    mainClass.run(args)
  }
}

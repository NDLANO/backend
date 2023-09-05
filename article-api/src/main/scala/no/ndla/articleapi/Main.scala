/*
 * Part of NDLA article-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import cats.effect.{ExitCode, IO, IOApp}
import no.ndla.common.Environment.setPropsFromEnv

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    setPropsFromEnv()
    val props     = new ArticleApiProperties
    val mainClass = new MainClass(props)
    mainClass.run()
  }
}

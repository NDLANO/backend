/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import cats.effect.{ExitCode, IO, IOApp}
import no.ndla.common.Environment.setPropsFromEnv

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    setPropsFromEnv()
    val props     = new FrontpageApiProperties
    val mainClass = new MainClass(props)
    mainClass.run()
  }
}

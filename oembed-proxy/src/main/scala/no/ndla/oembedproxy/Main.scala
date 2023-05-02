/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import cats.effect.{ExitCode, IO, IOApp}
import no.ndla.common.Environment.setPropsFromEnv

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    setPropsFromEnv()
    val props     = new OEmbedProxyProperties
    val mainClass = new MainClass(props)
    mainClass.run(args)
  }
}

/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import cats.data.Kleisli
import cats.effect.IO
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain
import org.http4s.{Request, Response}

class MainClass(override val props: AudioApiProperties) extends NdlaTapirMain {
  private val componentRegistry                            = new ComponentRegistry(props)
  override val app: Kleisli[IO, Request[IO], Response[IO]] = componentRegistry.routes

  private def warmupRequest = (path, params) => Warmup.warmupRequest(props.ApplicationPort, path, params)
  override def warmup(): Unit = {
    warmupRequest("/audio-api/v1/audio", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/audio-api/v1/audio/1", Map("language" -> "nb"))
    warmupRequest("/audio-api/v1/series", Map("language" -> "nb"))
    warmupRequest("/audio-api/v1/series/1", Map("language" -> "nb"))
    warmupRequest("/health", Map.empty)
  }

  override def beforeStart(): Unit = {
    logger.info("Starting DB Migration")
    val dBstartMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - dBstartMillis} ms")
  }
}

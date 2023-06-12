/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.data.Kleisli
import cats.effect.IO
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain
import org.http4s.{Request, Response}

class MainClass(override val props: FrontpageApiProperties) extends NdlaTapirMain {
  private val componentRegistry                            = new ComponentRegistry(props)
  override val app: Kleisli[IO, Request[IO], Response[IO]] = componentRegistry.routes

  override def beforeStart(): Unit = {
    logger.info("Starting DB Migration")
    val dBstartMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate(): Unit
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - dBstartMillis} ms")
  }

  private def warmupRequest = (path: String) => Warmup.warmupRequest(props.ApplicationPort, path, Map.empty)
  override def warmup(): Unit = {
    warmupRequest("/frontpage-api/v1/frontpage")
    warmupRequest("/frontpage-api/v1/subjectpage/1")
    warmupRequest("/health")

    componentRegistry.healthController.setWarmedUp()
  }
}

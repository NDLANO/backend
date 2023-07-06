/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import cats.data.Kleisli
import cats.effect.IO
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain
import org.http4s.{Request, Response}

class MainClass(override val props: ArticleApiProperties) extends NdlaTapirMain {
  val componentRegistry                                    = new ComponentRegistry(props)
  override val app: Kleisli[IO, Request[IO], Response[IO]] = componentRegistry.routes

  private def warmupRequest = (path: String, options: Map[String, String]) =>
    Warmup.warmupRequest(props.ApplicationPort, path, options)
  override def warmup(): Unit = {
    warmupRequest("/article-api/v2/articles", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/article-api/v2/articles/1", Map("language" -> "nb"))
    warmupRequest("/article-api/v2/articles/ids/", Map("ids" -> "100,1000,2000,3000", "fallback" -> "true"))
    warmupRequest("/health", Map.empty)
  }

  override def beforeStart(): Unit = {
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
  }
}

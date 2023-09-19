/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import cats.effect.IO
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: ArticleApiProperties) extends NdlaTapirMain[Eff] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path: String, options: Map[String, String]) =>
    Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/article-api/v2/articles", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/article-api/v2/articles/1", Map("language" -> "nb"))
    warmupRequest("/article-api/v2/articles/ids/", Map("ids" -> "100,1000,2000,3000", "fallback" -> "true"))
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  override def beforeStart(): Unit = {
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
  }

  override def startServer(name: String, port: Int)(warmupFunc: => Unit): IO[Unit] =
    componentRegistry.Routes.startJdkServer(name, port)(warmupFunc)
}

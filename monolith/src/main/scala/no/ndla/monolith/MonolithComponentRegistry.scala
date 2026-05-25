/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith

import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  SwaggerInfo,
  TapirApplication,
  TapirController,
  TapirHealthController,
}

/** Orchestrating registry that constructs every per-app [[no.ndla.network.tapir.TapirApplication]] in this repo and
  * merges their controllers into a single Netty server.
  *
  * Inter-app calls in this initial version still go via HTTP loopback — each per-app `ComponentRegistry` constructs its
  * default `*HttpClient` which talks to `localhost:${APPLICATION_PORT}`. Real in-process clients land incrementally by
  * overriding the `buildXApiClient` hooks on each per-app `ComponentRegistry`.
  */
class MonolithComponentRegistry(properties: MonolithProperties) extends TapirApplication[MonolithProperties] {

  given props: MonolithProperties = properties

  // Per-app component registries. lazy val so per-app initialisation can be overridden lazily and so cross-app
  // references (when we add in-process clients) don't trip on construction order.
  lazy val articleApi: no.ndla.articleapi.ComponentRegistry =
    new no.ndla.articleapi.ComponentRegistry(properties.article)
  lazy val audioApi: no.ndla.audioapi.ComponentRegistry     = new no.ndla.audioapi.ComponentRegistry(properties.audio)
  lazy val conceptApi: no.ndla.conceptapi.ComponentRegistry =
    new no.ndla.conceptapi.ComponentRegistry(properties.concept)
  lazy val draftApi: no.ndla.draftapi.ComponentRegistry         = new no.ndla.draftapi.ComponentRegistry(properties.draft)
  lazy val frontpageApi: no.ndla.frontpageapi.ComponentRegistry =
    new no.ndla.frontpageapi.ComponentRegistry(properties.frontpage)
  lazy val imageApi: no.ndla.imageapi.ComponentRegistry               = new no.ndla.imageapi.ComponentRegistry(properties.image)
  lazy val learningpathApi: no.ndla.learningpathapi.ComponentRegistry =
    new no.ndla.learningpathapi.ComponentRegistry(properties.learningpath)
  lazy val myndlaApi: no.ndla.myndlaapi.ComponentRegistry     = new no.ndla.myndlaapi.ComponentRegistry(properties.myndla)
  lazy val oembedProxy: no.ndla.oembedproxy.ComponentRegistry =
    new no.ndla.oembedproxy.ComponentRegistry(properties.oembed)
  lazy val searchApi: no.ndla.searchapi.ComponentRegistry = new no.ndla.searchapi.ComponentRegistry(properties.search)

  private lazy val allAppCrs: List[TapirApplication[?]] = List(
    articleApi,
    audioApi,
    conceptApi,
    draftApi,
    frontpageApi,
    imageApi,
    learningpathApi,
    myndlaApi,
    oembedProxy,
    searchApi,
  )

  given clock: Clock                     = new Clock
  given errorHelpers: ErrorHelpers       = new ErrorHelpers
  given ndlaClient: NdlaClient           = new NdlaClient
  given myNDLAApiClient: MyNDLAApiClient = new MyNDLAApiClient

  // Each per-app CR carries its own ControllerErrorHandling with domain-specific PartialFunctions; compose them so
  // every domain error gets matched by its owning app's handler.
  given errorHandling: ErrorHandling = new CompositeErrorHandling(
    Seq(
      articleApi.errorHandling,
      audioApi.errorHandling,
      conceptApi.errorHandling,
      draftApi.errorHandling,
      frontpageApi.errorHandling,
      imageApi.errorHandling,
      learningpathApi.errorHandling,
      myndlaApi.errorHandling,
      oembedProxy.errorHandling,
      searchApi.errorHandling,
    )
  )

  given swaggerInfo: SwaggerInfo = SwaggerInfo(
    prefix = "",
    description = "Aggregated API surface for all NDLA backend services running in a single JVM.",
  )

  given healthController: TapirHealthController = new TapirHealthController

  // Pull every TapirController out of each per-app CR's swagger, drop per-app Swagger/Health (we provide merged
  // versions). Keep everything else.
  private lazy val perAppControllers: List[TapirController] = allAppCrs
    .flatMap(_.swagger.allServices)
    .filterNot { c =>
      c.isInstanceOf[SwaggerController] || c.isInstanceOf[TapirHealthController]
    }

  given swagger: SwaggerController = new SwaggerController(perAppControllers*)

  given services: List[TapirController] = swagger.allServices :+ healthController
  given routes: Routes                  = new Routes

  /** Run every per-app Flyway migrator in declaration order. */
  def runAllMigrations(): Unit = {
    articleApi.migrator.migrate()
    audioApi.migrator.migrate()
    conceptApi.migrator.migrate()
    draftApi.migrator.migrate()
    frontpageApi.migrator.migrate()
    imageApi.migrator.migrate()
    learningpathApi.migrator.migrate()
    myndlaApi.migrator.migrate()
    // oembed-proxy and search-api have no DB / migrator
  }

  /** Endpoints to hit during warmup so each app's route handlers are JIT-warmed before traffic ramps up. */
  val warmupEndpoints: List[(String, Map[String, String])] = List(
    "/article-api/v2/articles"           -> Map("query" -> "norge", "fallback" -> "true"),
    "/draft-api/v1/drafts/"              -> Map.empty,
    "/audio-api/v1/audio"                -> Map.empty,
    "/concept-api/v1/concepts/"          -> Map.empty,
    "/frontpage-api/v1/frontpage/"       -> Map.empty,
    "/image-api/v3/images/"              -> Map.empty,
    "/learningpath-api/v2/learningpaths" -> Map.empty,
    "/myndla-api/v1/users/"              -> Map.empty,
    "/oembed-proxy/v1/oembed"            -> Map("url" -> "https://ndla.no"),
    "/search-api/v1/search"              -> Map.empty,
    "/health"                            -> Map.empty,
  )

}

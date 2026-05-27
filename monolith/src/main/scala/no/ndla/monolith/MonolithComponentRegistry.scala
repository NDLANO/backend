/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith

import no.ndla.common.Clock
import no.ndla.monolith.inprocess.*
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  LegacyPrefixAlias,
  Routes,
  SwaggerController,
  SwaggerInfo,
  TapirApplication,
  TapirController,
  TapirHealthController,
}

/** Orchestrating registry that constructs every per-app [[no.ndla.network.tapir.TapirApplication]] in this repo and
  * merges their controllers into a single Netty server. Each consumer-side `*ApiClient` for an in-repo peer is
  * overridden to use an in-process implementation that calls the producer's services directly, skipping JSON ser/de and
  * the Netty hop. External clients (Taxonomy, H5P, Feide, Matomo, Grep) stay HTTP.
  */
class MonolithComponentRegistry(properties: MonolithProperties) extends TapirApplication[MonolithProperties] {

  given props: MonolithProperties = properties

  // Per-app component registries. lazy val so cross-app references through in-process clients resolve via the
  // by-name constructors on each `*InProcessClient`, regardless of init order.
  lazy val frontpageApi: no.ndla.frontpageapi.ComponentRegistry =
    new no.ndla.frontpageapi.ComponentRegistry(properties.frontpage)

  lazy val imageApi: no.ndla.imageapi.ComponentRegistry = new no.ndla.imageapi.ComponentRegistry(properties.image)

  lazy val audioApi: no.ndla.audioapi.ComponentRegistry = new no.ndla.audioapi.ComponentRegistry(properties.audio)

  lazy val conceptApi: no.ndla.conceptapi.ComponentRegistry =
    new no.ndla.conceptapi.ComponentRegistry(properties.concept)

  lazy val oembedProxy: no.ndla.oembedproxy.ComponentRegistry =
    new no.ndla.oembedproxy.ComponentRegistry(properties.oembed)

  lazy val articleApi: no.ndla.articleapi.ComponentRegistry =
    new no.ndla.articleapi.ComponentRegistry(properties.article) {
      override protected def buildFrontpageApiClient = new FrontpageForArticleApiInProcessClient(frontpageApi)
      override protected def buildImageApiClient     = new ImageForArticleApiInProcessClient(imageApi)
    }

  lazy val searchApi: no.ndla.searchapi.ComponentRegistry = new no.ndla.searchapi.ComponentRegistry(properties.search) {
    override protected def buildArticleApiClient      = new ArticleForSearchApiInProcessClient(articleApi)
    override protected def buildDraftApiClient        = new DraftForSearchApiInProcessClient(draftApi)
    override protected def buildDraftConceptApiClient = new ConceptForSearchApiInProcessClient(conceptApi)
    override protected def buildLearningPathApiClient =
      new LearningpathForSearchApiInProcessClient(learningpathApi, baseUrl = "in-process://learningpath-api")
  }

  lazy val learningpathApi: no.ndla.learningpathapi.ComponentRegistry =
    new no.ndla.learningpathapi.ComponentRegistry(properties.learningpath) {
      override protected def buildSearchApiClient   = new SearchForLearningpathApiInProcessClient(searchApi)
      override protected def buildOembedProxyClient = new OembedForLearningpathApiInProcessClient(oembedProxy)
    }

  lazy val draftApi: no.ndla.draftapi.ComponentRegistry = new no.ndla.draftapi.ComponentRegistry(properties.draft) {
    self =>
    override protected def buildArticleApiClient      = new ArticleForDraftApiInProcessClient(articleApi, self)
    override protected def buildImageApiClient        = new ImageForDraftApiInProcessClient(imageApi)
    override protected def buildLearningpathApiClient = new LearningpathForDraftApiInProcessClient(learningpathApi)
  }

  lazy val myndlaApi: no.ndla.myndlaapi.ComponentRegistry = new no.ndla.myndlaapi.ComponentRegistry(properties.myndla) {
    override protected def buildLearningPathApiClient = new LearningpathForMyndlaApiInProcessClient(learningpathApi)
    override protected def buildSearchApiClient       = new SearchForMyndlaApiInProcessClient(searchApi)
  }

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
  // versions). Also drop LegacyPrefixAlias instances: each per-app CR registers an alias of its InternController at
  // the bare `intern` prefix to keep microservice-mode callers working during the per-app prefix transition; the
  // monolith only serves the canonical `intern/<app-name>` paths so the legacy aliases would collide across apps.
  private lazy val perAppControllers: List[TapirController] = allAppCrs
    .flatMap(_.swagger.allServices)
    .filterNot { c =>
      c.isInstanceOf[SwaggerController] || c.isInstanceOf[TapirHealthController] ||
      c.isInstanceOf[LegacyPrefixAlias]
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

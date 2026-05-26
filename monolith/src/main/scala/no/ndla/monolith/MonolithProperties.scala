/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.audioapi.AudioApiProperties
import no.ndla.common.auth.Permission
import no.ndla.common.configuration.BaseProps
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.draftapi.DraftApiProperties
import no.ndla.frontpageapi.FrontpageApiProperties
import no.ndla.imageapi.ImageApiProperties
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.myndlaapi.MyNdlaApiProperties
import no.ndla.oembedproxy.OEmbedProxyProperties
import no.ndla.searchapi.SearchApiProperties

/** Aggregates the per-app properties so a single JVM can boot every Scala *-api in this repo.
  *
  * Each per-app props instance reads the same env vars it does in microservice mode. APPLICATION_PORT is read once and
  * is shared across all apps because we bind a single Netty server.
  */
class MonolithProperties extends BaseProps {

  override def ApplicationName: String          = "monolith"
  override def ApplicationPort: Int             = propOrElseInt("APPLICATION_PORT", 80)
  override def ndlaAuth0Scopes: Seq[Permission] = Permission.values.toSeq

  val article: ArticleApiProperties           = new ArticleApiProperties
  val audio: AudioApiProperties               = new AudioApiProperties
  val concept: ConceptApiProperties           = new ConceptApiProperties
  val draft: DraftApiProperties               = new DraftApiProperties
  val frontpage: FrontpageApiProperties       = new FrontpageApiProperties
  val image: ImageApiProperties               = new ImageApiProperties
  val learningpath: LearningpathApiProperties = new LearningpathApiProperties
  val myndla: MyNdlaApiProperties             = new MyNdlaApiProperties
  val oembed: OEmbedProxyProperties           = new OEmbedProxyProperties
  val search: SearchApiProperties             = new SearchApiProperties

  private def perApps: Seq[BaseProps] =
    Seq(article, audio, concept, draft, frontpage, image, learningpath, myndla, oembed, search)

  // Each per-app props sets `APPLICATION_NAME` as a JVM-wide system property during its own construction; the last
  // one wins. Re-assert the monolith's name here so non-request log lines (startup, shutdown, background tasks)
  // are tagged "monolith" rather than whichever per-app props happened to initialise last. Per-request log lines
  // get their proper app name via MDC set by `Routes` middleware.
  System.setProperty("APPLICATION_NAME", ApplicationName): Unit

  override def throwIfFailedProps(): Unit = {
    super.throwIfFailedProps()
    perApps.foreach(_.throwIfFailedProps())
  }

  private def propOrElseInt(key: String, default: Int): Int = scala
    .util
    .Properties
    .propOrNone(key)
    .flatMap(_.toIntOption)
    .getOrElse(default)
}

/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.articleapi.integration.FrontpageApiClient
import no.ndla.common.model.api.FrontPageDTO

import scala.util.Try

/** In-process implementation of article-api's [[FrontpageApiClient]] trait that delegates to frontpage-api's
  * [[no.ndla.frontpageapi.service.ReadService]] directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.frontpageapi.ComponentRegistry]] and [[no.ndla.articleapi.ComponentRegistry]] in the monolith.
  */
class FrontpageForArticleApiInProcessClient(producerCr: => no.ndla.frontpageapi.ComponentRegistry)
    extends FrontpageApiClient {

  override def getFrontpage: Try[FrontPageDTO] = producerCr.readService.getFrontPage
}

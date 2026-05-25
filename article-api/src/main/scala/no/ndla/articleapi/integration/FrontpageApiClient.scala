/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import no.ndla.common.model.api.FrontPageDTO

import scala.util.Try

trait FrontpageApiClient {
  def getFrontpage: Try[FrontPageDTO]
}

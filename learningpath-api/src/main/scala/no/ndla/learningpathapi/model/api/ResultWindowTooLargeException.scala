/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.Props

case class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
object ResultWindowTooLargeException {
  def default(using props: Props): ResultWindowTooLargeException =
    ResultWindowTooLargeException(
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
    )
}

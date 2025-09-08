/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import no.ndla.searchapi.Props

case class InvalidIndexBodyException(
    message: String = "Unable to index the requested document because body was invalid."
) extends RuntimeException(message)

case class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
object ResultWindowTooLargeException {
  def default(using props: Props): ResultWindowTooLargeException = {
    val WINDOW_TOO_LARGE_DESCRIPTION: String =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
    new ResultWindowTooLargeException(WINDOW_TOO_LARGE_DESCRIPTION)
  }
}
class ApiSearchException(val apiName: String, message: String) extends RuntimeException(message)
case class GrepException(message: String)                      extends RuntimeException(message)

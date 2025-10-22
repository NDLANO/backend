/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import no.ndla.conceptapi.Props
import no.ndla.network.tapir.ErrorHelpers

class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
object ResultWindowTooLargeException {
  def WINDOW_TOO_LARGE_DESCRIPTION(using props: Props): String =
    s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

  def default(using props: Props): ResultWindowTooLargeException =
    new ResultWindowTooLargeException(WINDOW_TOO_LARGE_DESCRIPTION)
}

case class OptimisticLockException(message: String) extends RuntimeException(message)
object OptimisticLockException {
  def default(using help: ErrorHelpers): OptimisticLockException =
    new OptimisticLockException(help.RESOURCE_OUTDATED_DESCRIPTION)
}
case class IllegalStatusStateTransition(message: String) extends RuntimeException(message)
case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ConceptMissingIdException(message: String)     extends RuntimeException(message)
case class ConceptExistsAlreadyException(message: String) extends RuntimeException(message)

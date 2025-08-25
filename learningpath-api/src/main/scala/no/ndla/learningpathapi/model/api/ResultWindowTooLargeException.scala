/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.database.DataSource
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.domain.{ImportException, InvalidLpStatusException, OptimisticLockException}
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, TapirErrorHandling}
import no.ndla.search.model.domain.ElasticIndexingException
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException
import no.ndla.common.errors.OperationNotAllowedException

case class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
object ResultWindowTooLargeException {
  def default(using props: Props): ResultWindowTooLargeException =
    ResultWindowTooLargeException(
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
    )
}

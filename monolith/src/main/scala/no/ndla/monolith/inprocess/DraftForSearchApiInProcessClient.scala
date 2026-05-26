/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import io.circe.Decoder
import no.ndla.common.model.domain.draft.Draft
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.DraftApiClient
import no.ndla.searchapi.model.domain.DomainDumpResults

import scala.util.Try

/** In-process implementation of [[DraftApiClient]] used by the monolith. Routes every method call directly into the
  * draft-api producer's [[no.ndla.draftapi.service.ReadService]], bypassing HTTP entirely.
  */
class DraftForSearchApiInProcessClient(producerCr: => no.ndla.draftapi.ComponentRegistry)(using
    ndlaClient: NdlaClient,
    props: Props,
) extends DraftApiClient {
  // These mirror the HTTP impl's values for diagnostic/logging purposes; they are never actually used to make a
  // network call because every method below is overridden.
  override val name           = "drafts"
  override val baseUrl        = "in-process://draft-api"
  override val searchPath     = "draft-api/v1/drafts"
  override val dumpDomainPath = "intern/dump/article"

  override def getSingle(id: Long)(implicit d: Decoder[Draft]): Try[Draft] = producerCr
    .readService
    .getSingleArticleForDump(id)

  override protected def getChunk(page: Int, pageSize: Int)(implicit d: Decoder[Draft]): Try[DomainDumpResults[Draft]] =
    producerCr
      .readService
      .getArticleDomainDump(page, pageSize)
      .map(dump => DomainDumpResults(dump.totalCount, dump.page, dump.pageSize, dump.results))
}

/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import io.circe.Decoder
import no.ndla.common.model.domain.concept.Concept
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.DraftConceptApiClient
import no.ndla.searchapi.model.domain.DomainDumpResults

import scala.util.{Success, Try}

/** In-process implementation of [[DraftConceptApiClient]] used by the monolith. Routes every method call directly into
  * the concept-api producer's [[no.ndla.conceptapi.service.ReadService]], bypassing HTTP entirely.
  */
class ConceptForSearchApiInProcessClient(producerCr: => no.ndla.conceptapi.ComponentRegistry)(using
    ndlaClient: NdlaClient,
    props: Props,
) extends DraftConceptApiClient {
  // These mirror the HTTP impl's values for diagnostic/logging purposes; they are never actually used to make a
  // network call because every method below is overridden.
  override val name           = "concepts"
  override val baseUrl        = "in-process://concept-api"
  override val searchPath     = "concept-api/v1/drafts"
  override val dumpDomainPath = "intern/dump/draft-concept"

  override def getSingle(id: Long)(implicit d: Decoder[Concept]): Try[Concept] = producerCr
    .readService
    .getSingleDraftConceptForDump(id)

  override protected def getChunk(page: Int, pageSize: Int)(implicit
      d: Decoder[Concept]
  ): Try[DomainDumpResults[Concept]] = {
    val dump = producerCr.readService.getDraftConceptDomainDump(page, pageSize)
    Success(DomainDumpResults(dump.totalCount, dump.page, dump.pageSize, dump.results))
  }
}

/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.common.configuration.BaseProps
import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.network.NdlaClient
import no.ndla.network.clients.FrontpageApiClient

import scala.util.Try

/** In-process implementation of [[FrontpageApiClient]] that delegates to frontpage-api's
  * [[no.ndla.frontpageapi.service.ReadService]] directly, skipping the HTTP/JSON ser-de hop. Used by search-api in the
  * monolith for subject-page lookups during indexing.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the peer CRs and
  * [[no.ndla.frontpageapi.ComponentRegistry]].
  *
  * Subclasses the concrete [[FrontpageApiClient]] (rather than a trait) because peer services take the concrete client
  * type as a dependency; the HTTP URL computed by the superclass is never used because `getSubjectPage` is overridden.
  */
class FrontpageForSearchApiInProcessClient(producerCr: => no.ndla.frontpageapi.ComponentRegistry)(using
    props: BaseProps,
    ndlaClient: NdlaClient,
) extends FrontpageApiClient {

  override def getSubjectPage(id: Long): Try[SubjectPage] = producerCr.readService.domainSubjectPage(id)
}

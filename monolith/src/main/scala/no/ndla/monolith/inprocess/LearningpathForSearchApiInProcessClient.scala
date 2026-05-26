/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.common.model.domain.learningpath.LearningPathStatus.DELETED
import no.ndla.common.model.domain.learningpath.LearningPathVerificationStatus.CREATED_BY_NDLA
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.LearningPathApiClient
import no.ndla.searchapi.model.domain.DomainDumpResults

import scala.util.{Success, Try}

/** In-process implementation of search-api's [[LearningPathApiClient]] trait that delegates to learningpath-api's
  * [[no.ndla.learningpathapi.service.ReadService]] / repository directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.learningpathapi.ComponentRegistry]] and [[no.ndla.searchapi.ComponentRegistry]] in the monolith.
  *
  * The base [[no.ndla.searchapi.integration.SearchApiClient]] trait still requires `NdlaClient` and `Props` as
  * constructor `using` parameters even though this implementation does not perform any HTTP calls; pass through the
  * search-api CR's instances so the base trait's `get` method (unused here) remains well-formed.
  */
class LearningpathForSearchApiInProcessClient(
    learningpathApiCr: => no.ndla.learningpathapi.ComponentRegistry,
    override val baseUrl: String,
)(using ndlaClient: NdlaClient, props: Props)
    extends LearningPathApiClient
    with StrictLogging {

  override val searchPath: String     = "learningpath-api/v2/learningpaths"
  override val name: String           = "learningpaths"
  override val dumpDomainPath: String = "intern/dump/learningpath"

  override def getSingle(id: Long)(implicit d: Decoder[LearningPath]): Try[LearningPath] =
    learningpathApiCr.learningPathRepository.withId(id) match {
      case Some(value) => Success(value)
      case None        => scala.util.Failure(new RuntimeException(s"Could not fetch single $name (id: $id) in-process"))
    }

  override protected def getChunk(page: Int, pageSize: Int)(implicit
      d: Decoder[LearningPath]
  ): Try[DomainDumpResults[LearningPath]] = Try {
    val dump = learningpathApiCr.readService.getLearningPathDomainDump(page, pageSize, onlyIncludePublished = false)
    // Mirror the same filtering the HTTP impl applies to the response payload before returning to callers.
    val filtered = dump.results.filter(r => DELETED != r.status).filter(_.verificationStatus == CREATED_BY_NDLA)
    logger.info(s"Fetched chunk of ${filtered.size} $name in-process (page=$page, pageSize=$pageSize)")
    DomainDumpResults(dump.totalCount, dump.page, dump.pageSize, filtered)
  }
}

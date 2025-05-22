/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.common.model.domain.learningpath.LearningPathStatus.{PUBLISHED, SUBMITTED, UNLISTED}
import no.ndla.common.model.domain.learningpath.LearningPathVerificationStatus.CREATED_BY_NDLA
import no.ndla.network.NdlaClient
import no.ndla.network.model.RequestInfo
import no.ndla.searchapi.model.domain.DomainDumpResults

import scala.util.{Failure, Success, Try}

trait LearningPathApiClient {
  this: NdlaClient & StrictLogging & SearchApiClient =>
  val learningPathApiClient: LearningPathApiClient

  class LearningPathApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath     = "learningpath-api/v2/learningpaths"
    override val name           = "learningpaths"
    override val dumpDomainPath = "intern/dump/learningpath"

    override protected def getChunk[T: Decoder](page: Int, pageSize: Int): Try[DomainDumpResults[T]] = {
      val params = Map(
        "page"           -> page.toString,
        "page-size"      -> pageSize.toString,
        "only-published" -> "false"
      )
      val reqs = RequestInfo.fromThreadContext()
      reqs.setThreadContextRequestInfo()
      get[DomainDumpResults[T]](dumpDomainPath, params, timeout = 120000) match {
        case Success(result) =>
          val results = result.results.asInstanceOf[Seq[LearningPath]]
          val filtered = results
            .filter(r => List(PUBLISHED, UNLISTED, SUBMITTED).contains(r.status))
            .filter(_.verificationStatus == CREATED_BY_NDLA)
          logger.info(s"Fetched chunk of ${filtered.size} $name from ${baseUrl.addParams(params)}")
          Success(result.copy(results = filtered.asInstanceOf[Seq[T]]))
        case Failure(ex) =>
          logger.error(
            s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' from '$baseUrl/$dumpDomainPath'"
          )
          Failure(ex)
      }
    }
  }
}

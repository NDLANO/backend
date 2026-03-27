/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.controller

import cats.implicits.*
import io.circe.generic.auto.*
import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.frontpageapi.model.api.*
import no.ndla.frontpageapi.service.{MatomoService, ReadService}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirController}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

class InternController(using
    readService: ReadService,
    matomoService: MatomoService,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
) extends TapirController {
  import errorHandling.*
  override val prefix: EndpointInput[Unit] = "intern"
  override val enableSwagger               = false

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    endpoint
      .get
      .in("subjectpage" / "external" / path[String]("externalId").description("old NDLA node id"))
      .summary("Get subject page id from external id")
      .out(jsonBody[SubjectPageIdDTO])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { nid =>
        readService.getIdFromExternalId(nid) match {
          case Success(Some(id)) => id.asRight
          case Success(None)     => errorHelpers.notFound.asLeft
          case Failure(ex)       => returnLeftError(ex)
        }
      },
    endpoint
      .get
      .in("dump" / "subjectpage")
      .in(query[Int]("page").default(1))
      .in(query[Int]("page-size").default(100))
      .out(jsonBody[SubjectPageDomainDumpDTO])
      .serverLogicPure { case (pageNo, pageSize) =>
        readService.getSubjectPageDomainDump(pageNo, pageSize).asRight
      },
    endpoint
      .get
      .in("dump" / "subjectpage" / path[Long]("subjectId"))
      .out(jsonBody[SubjectPage])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { subjectId =>
        readService.domainSubjectPage(subjectId)
      },
    endpoint
      .post
      .in("matomo" / "popular-articles" / path[Long]("subjectId"))
      .in(query[String]("subjectSlug").description("The subject slug used in Matomo dimension13"))
      .in(query[Option[Int]]("limit").description("Max number of articles to fetch"))
      .summary("Trigger fetching popular articles from Matomo for a subject page")
      .out(jsonBody[PopularArticlesResultDTO])
      .errorOut(errorOutputsFor(400, 404, 502))
      .serverLogicPure { case (subjectId, subjectSlug, limit) =>
        matomoService.updatePopularArticlesForSubject(subjectId, subjectSlug, limit.getOrElse(20)) match {
          case Success(count) => PopularArticlesResultDTO(subjectId, count).asRight
          case Failure(ex)    => returnLeftError(ex)
        }
      },
  )

}

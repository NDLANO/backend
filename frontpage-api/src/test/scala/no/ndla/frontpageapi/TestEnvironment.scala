/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.frontpageapi.controller.{
  ControllerErrorHandling,
  FilmPageController,
  FrontPageController,
  SubjectPageController
}
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPage, DBFrontPage, DBSubjectPage}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController
}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[FrontpageApiProperties] with MockitoSugar {
  implicit lazy val props: FrontpageApiProperties   = new FrontpageApiProperties
  implicit lazy val clock: Clock                    = mock[Clock]
  implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  implicit lazy val routes: Routes                  = new Routes
  implicit lazy val services: List[TapirController] = List.empty

  implicit lazy val migrator: DBMigrator   = mock[DBMigrator]
  implicit lazy val dataSource: DataSource = mock[DataSource]

  implicit lazy val dBSubjectPage: DBSubjectPage     = mock[DBSubjectPage]
  implicit lazy val dBFrontPage: DBFrontPage         = mock[DBFrontPage]
  implicit lazy val dBFilmFrontPage: DBFilmFrontPage = mock[DBFilmFrontPage]

  implicit lazy val filmPageController: FilmPageController           = mock[FilmPageController]
  implicit lazy val subjectPageController: SubjectPageController     = mock[SubjectPageController]
  implicit lazy val frontPageController: FrontPageController         = mock[FrontPageController]
  implicit lazy val subjectPageRepository: SubjectPageRepository     = mock[SubjectPageRepository]
  implicit lazy val frontPageRepository: FrontPageRepository         = mock[FrontPageRepository]
  implicit lazy val filmFrontPageRepository: FilmFrontPageRepository = mock[FilmFrontPageRepository]
  implicit lazy val healthController: TapirHealthController          = mock[TapirHealthController]
  implicit lazy val readService: ReadService                         = mock[ReadService]
  implicit lazy val writeService: WriteService                       = mock[WriteService]
  implicit lazy val converterService: ConverterService               = mock[ConverterService]

  implicit lazy val ndlaClient: NdlaClient           = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  implicit lazy val swagger: SwaggerController = mock[SwaggerController]
}

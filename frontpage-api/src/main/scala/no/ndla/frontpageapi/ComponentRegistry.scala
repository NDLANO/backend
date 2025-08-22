/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.frontpageapi.controller.*
import no.ndla.frontpageapi.model.api.ErrorHandling
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPage, DBFrontPage, DBSubjectPage}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirErrorHandling,
  TapirHealthController
}

class ComponentRegistry(properties: FrontpageApiProperties) extends TapirApplication[FrontpageApiProperties] {
  given props: FrontpageApiProperties         = properties
  given routes: Routes                        = new Routes
  override given errorHandling: ErrorHandling = new ErrorHandling
  given migrator: DBMigrator                  = DBMigrator()
  given dataSource: DataSource                = DataSource.getDataSource

  given clock: Clock = new Clock

  given DBSubjectPage                                    = new DBSubjectPage
  given DBFrontPage                                      = new DBFrontPage
  given DBFilmFrontPage                                  = new DBFilmFrontPage
  given subjectPageRepository: SubjectPageRepository     = new SubjectPageRepository
  given frontPageRepository: FrontPageRepository         = new FrontPageRepository
  given filmFrontPageRepository: FilmFrontPageRepository = new FilmFrontPageRepository
  given converterService: ConverterService               = new ConverterService

  given readService: ReadService   = new ReadService
  given writeService: WriteService = new WriteService

  given subjectPageController: SubjectPageController = new SubjectPageController
  given frontPageController: FrontPageController     = new FrontPageController
  given filmPageController: FilmPageController       = new FilmPageController
  given internController: InternController           = new InternController
  given healthController: TapirHealthController      = new TapirHealthController

  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  given ndlaClient: NdlaClient           = new NdlaClient

  val swagger = new SwaggerController(
    List(
      subjectPageController,
      frontPageController,
      filmPageController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  given services: List[TapirController] = swagger.getServices()
}

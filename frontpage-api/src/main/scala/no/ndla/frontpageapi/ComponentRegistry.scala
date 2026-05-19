/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.frontpageapi.controller.*
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPage, DBFrontPage, DBSubjectPage}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.jwt.{DefaultJwsKeySelectorFactory, JwsKeySelectorFactory}
import no.ndla.network.tapir.auth.{CombinedAuth, FeideAuth, NdlaAuth}
import no.ndla.network.tapir.*

class ComponentRegistry(properties: FrontpageApiProperties) extends TapirApplication[FrontpageApiProperties] {
  given props: FrontpageApiProperties = properties
  given clock: Clock                  = new Clock
  given errorHelpers: ErrorHelpers    = new ErrorHelpers
  given errorHandling: ErrorHandling  = new ControllerErrorHandling
  given dataSource: DataSource        = DataSource.getDataSource
  given migrator: DBMigrator          = DBMigrator()
  given ndlaClient: NdlaClient        = new NdlaClient
  given dbUtility: DBUtility          = new DBUtility

  given DBSubjectPage                                    = new DBSubjectPage
  given DBFrontPage                                      = new DBFrontPage
  given DBFilmFrontPage                                  = new DBFilmFrontPage
  given subjectPageRepository: SubjectPageRepository     = new SubjectPageRepository
  given frontPageRepository: FrontPageRepository         = new FrontPageRepository
  given filmFrontPageRepository: FilmFrontPageRepository = new FilmFrontPageRepository
  given converterService: ConverterService               = new ConverterService
  given myndlaApiClient: MyNDLAApiClient                 = new MyNDLAApiClient
  given jwsKeySelectorFactory: JwsKeySelectorFactory     = DefaultJwsKeySelectorFactory
  given ndlaAuth: NdlaAuth                               = NdlaAuth()
  given feideAuth: FeideAuth                             = FeideAuth()
  given combinedAuth: CombinedAuth                       = CombinedAuth()

  given readService: ReadService   = new ReadService
  given writeService: WriteService = new WriteService

  given subjectPageController: SubjectPageController = new SubjectPageController
  given frontPageController: FrontPageController     = new FrontPageController
  given filmPageController: FilmPageController       = new FilmPageController
  given internController: InternController           = new InternController
  given healthController: TapirHealthController      = new TapirHealthController

  given swaggerInfo: SwaggerInfo =
    SwaggerInfo(prefix = "frontpage-api", description = "Service for fetching frontpage data")
  given swagger: SwaggerController = new SwaggerController(
    subjectPageController,
    frontPageController,
    filmPageController,
    internController,
    healthController,
  )

  given services: List[TapirController] = swagger.allServices
  given routes: Routes                  = new Routes
}

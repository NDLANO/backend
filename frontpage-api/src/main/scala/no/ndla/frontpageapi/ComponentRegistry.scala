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
import no.ndla.frontpageapi.service.{ConverterService, MatomoService, ReadService, WriteService}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.matomo.MatomoApiClient
import no.ndla.network.clients.{MyNDLAApiClient, TaxonomyApiClient}
import no.ndla.network.jwt.{DefaultJwsKeySelectorFactory, JwsKeySelectorFactory}
import no.ndla.network.tapir.*
import no.ndla.network.tapir.auth.NdlaAuth

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
  protected def buildMyNDLAApiClient: MyNDLAApiClient    = new MyNDLAApiClient
  given myndlaApiClient: MyNDLAApiClient                 = buildMyNDLAApiClient
  given taxonomyApiClient: TaxonomyApiClient             = new TaxonomyApiClient(props.TaxonomyUrl)
  given matomoApiClient: MatomoApiClient                 = new MatomoApiClient
  given matomoService: MatomoService                     = new MatomoService
  given jwsKeySelectorFactory: JwsKeySelectorFactory     = DefaultJwsKeySelectorFactory
  given ndlaAuth: NdlaAuth                               = NdlaAuth()

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
    new LegacyPrefixAlias(internController, "intern"),
    healthController,
  )

  given services: List[TapirController] = swagger.allServices
  given routes: Routes                  = new Routes
}

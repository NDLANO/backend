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
import no.ndla.network.tapir.TapirApplication

class ComponentRegistry(properties: FrontpageApiProperties) extends TapirApplication[FrontpageApiProperties] {
  given props: FrontpageApiProperties = properties
  given migrator: DBMigrator          = DBMigrator()
  given dataSource: HikariDataSource  = DataSource.getDataSource

  given clock = new SystemClock

  given subjectPageRepository   = new SubjectPageRepository
  given frontPageRepository     = new FrontPageRepository
  given filmFrontPageRepository = new FilmFrontPageRepository

  given readService  = new ReadService
  given writeService = new WriteService

  given subjectPageController = new SubjectPageController
  given frontPageController   = new FrontPageController
  given filmPageController    = new FilmPageController
  given internController      = new InternController
  given healthController      = new TapirHealthController

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

  override def services: List[TapirController] = swagger.getServices()
}

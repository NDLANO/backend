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

class ComponentRegistry(properties: FrontpageApiProperties)
    extends TapirApplication
    with DataSource
    with SubjectPageRepository
    with FrontPageRepository
    with FilmFrontPageRepository
    with InternController
    with ReadService
    with WriteService
    with SubjectPageController
    with FrontPageController
    with FilmPageController
    with DBFilmFrontPage
    with DBSubjectPage
    with DBFrontPage
    with ErrorHandling
    with Clock
    with Props
    with DBMigrator
    with ConverterService
    with SwaggerDocControllerConfig {
  override val props: FrontpageApiProperties = properties
  override val migrator: DBMigrator          = DBMigrator()
  override val dataSource: HikariDataSource  = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  override val clock = new SystemClock

  override val subjectPageRepository   = new SubjectPageRepository
  override val frontPageRepository     = new FrontPageRepository
  override val filmFrontPageRepository = new FilmFrontPageRepository

  override val readService  = new ReadService
  override val writeService = new WriteService

  override val subjectPageController = new SubjectPageController
  override val frontPageController   = new FrontPageController
  override val filmPageController    = new FilmPageController
  override val internController      = new InternController
  val healthController               = new TapirHealthController

  override val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  override val ndlaClient: NdlaClient           = new NdlaClient

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

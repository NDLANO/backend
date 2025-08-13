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
  override lazy val props: FrontpageApiProperties = properties
  override lazy val migrator: DBMigrator          = DBMigrator()
  override lazy val dataSource: HikariDataSource  = DataSource.getHikariDataSource

  override lazy val clock = new SystemClock

  override lazy val subjectPageRepository   = new SubjectPageRepository
  override lazy val frontPageRepository     = new FrontPageRepository
  override lazy val filmFrontPageRepository = new FilmFrontPageRepository

  override lazy val readService  = new ReadService
  override lazy val writeService = new WriteService

  override lazy val subjectPageController = new SubjectPageController
  override lazy val frontPageController   = new FrontPageController
  override lazy val filmPageController    = new FilmPageController
  override lazy val internController      = new InternController
  override lazy val healthController      = new TapirHealthController

  override lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  override lazy val ndlaClient: NdlaClient           = new NdlaClient

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

/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.frontpageapi.controller._
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.api.ErrorHelpers
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPageData, DBFrontPageData, DBSubjectFrontPageData}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service, TapirHealthController}

class ComponentRegistry(properties: FrontpageApiProperties)
    extends DataSource
    with SubjectPageRepository
    with FrontPageRepository
    with FilmFrontPageRepository
    with InternController
    with ReadService
    with WriteService
    with SubjectPageController
    with FrontPageController
    with FilmPageController
    with DBFilmFrontPageData
    with DBSubjectFrontPageData
    with DBFrontPageData
    with ErrorHelpers
    with Clock
    with Props
    with DBMigrator
    with ConverterService
    with TapirHealthController
    with Routes[Eff]
    with NdlaMiddleware
    with SwaggerDocControllerConfig {
  override val props: FrontpageApiProperties = properties
  override val migrator                      = new DBMigrator
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
  val healthController               = new TapirHealthController[Eff]

  private val swagger = new SwaggerController(
    List(
      subjectPageController,
      frontPageController,
      filmPageController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override val services: List[Service[Eff]] = swagger.getServices()
}

/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.data.Kleisli
import cats.effect.IO
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.frontpageapi.controller._
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.api.ErrorHelpers
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPageData, DBFrontPageData, DBSubjectFrontPageData}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import org.http4s.{Request, Response}

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
    with HealthController
    with Service
    with Routes
    with NdlaMiddleware
    with SwaggerDocController {
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
  override val healthController      = new HealthController

  private val services: List[Service] = List(
    subjectPageController,
    frontPageController,
    filmPageController,
    internController,
    healthController
  )

  override val swaggerDocController = new SwaggerDocController(services)

  def routes: Kleisli[IO, Request[IO], Response[IO]] = Routes.build(services :+ swaggerDocController)
}

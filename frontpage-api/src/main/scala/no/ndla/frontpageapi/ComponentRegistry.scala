/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.IO
import com.zaxxer.hikari.HikariDataSource
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.frontpageapi.controller.{
  FilmPageController,
  FrontPageController,
  InternController,
  SubjectPageController
}
import no.ndla.frontpageapi.model.api.ErrorHelpers
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPageData, DBFrontPageData, DBSubjectFrontPageData}
import org.http4s.rho.swagger.syntax.{io => ioSwagger}

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
    with Props
    with DBMigrator
    with ConverterService {
  override val props: FrontpageApiProperties = properties
  override val migrator                      = new DBMigrator
  override val dataSource: HikariDataSource  = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  override val subjectPageRepository   = new SubjectPageRepository
  override val frontPageRepository     = new FrontPageRepository
  override val filmFrontPageRepository = new FilmFrontPageRepository

  override val readService  = new ReadService
  override val writeService = new WriteService

  override val subjectPageController = new SubjectPageController[IO](ioSwagger)
  override val frontPageController   = new FrontPageController[IO](ioSwagger)
  override val filmPageController    = new FilmPageController[IO](ioSwagger)
  override val internController      = new InternController[IO](ioSwagger)
}

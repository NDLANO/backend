/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.IO
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.frontpageapi.FrontpageApiProperties._
import no.ndla.frontpageapi.controller.{
  FilmPageController,
  FrontPageController,
  InternController,
  SubjectPageController
}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}
import org.http4s.rho.swagger.syntax.{io => ioSwagger}

object ComponentRegistry
    extends DataSource
    with SubjectPageRepository
    with FrontPageRepository
    with FilmFrontPageRepository
    with InternController
    with ReadService
    with WriteService
    with SubjectPageController
    with FrontPageController
    with FilmPageController {

  val dataSourceConfig = new HikariConfig()
  dataSourceConfig.setUsername(MetaUserName)
  dataSourceConfig.setPassword(MetaPassword)
  dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://$MetaServer:$MetaPort/$MetaResource")
  dataSourceConfig.setSchema(MetaSchema)
  dataSourceConfig.setMaximumPoolSize(MetaMaxConnections)
  override val dataSource = new HikariDataSource(dataSourceConfig)
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override val subjectPageRepository = new SubjectPageRepository
  override val frontPageRepository = new FrontPageRepository
  override val filmFrontPageRepository = new FilmFrontPageRepository

  override val readService = new ReadService
  override val writeService = new WriteService

  override val subjectPageController = new SubjectPageController[IO](ioSwagger)
  override val frontPageController = new FrontPageController[IO](ioSwagger)
  override val filmPageController = new FilmPageController[IO](ioSwagger)
  override val internController = new InternController[IO](ioSwagger)
}

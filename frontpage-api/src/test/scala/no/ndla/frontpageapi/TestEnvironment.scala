/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.frontpageapi.controller.{FilmPageController, NdlaMiddleware, Service, SubjectPageController}
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.api.ErrorHelpers
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPageData, DBFrontPageData, DBSubjectFrontPageData}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends MockitoSugar
    with DataSource
    with SubjectPageRepository
    with FrontPageRepository
    with FilmFrontPageRepository
    with FilmPageController
    with SubjectPageController
    with Service
    with NdlaMiddleware
    with ReadService
    with WriteService
    with ConverterService
    with Props
    with DBFilmFrontPageData
    with DBSubjectFrontPageData
    with DBFrontPageData
    with ErrorHelpers
    with Clock
    with DBMigrator
    with Routes {
  override val props = new FrontpageApiProperties

  override val clock: SystemClock      = mock[SystemClock]
  override val migrator: DBMigrator   = mock[DBMigrator]
  override val dataSource: HikariDataSource = mock[HikariDataSource]

  override val filmPageController: FilmPageController      = mock[FilmPageController]
  override val subjectPageController: SubjectPageController   = mock[SubjectPageController]
  override val subjectPageRepository: SubjectPageRepository   = mock[SubjectPageRepository]
  override val frontPageRepository: FrontPageRepository     = mock[FrontPageRepository]
  override val filmFrontPageRepository: FilmFrontPageRepository = mock[FilmFrontPageRepository]
  override val readService: ReadService             = mock[ReadService]
  override val writeService: WriteService            = mock[WriteService]
}

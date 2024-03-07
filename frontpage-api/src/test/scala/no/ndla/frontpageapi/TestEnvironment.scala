/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.frontpageapi.controller.{FilmPageController, FrontPageController, SubjectPageController}
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.api.ErrorHelpers
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPageData, DBFrontPageData, DBSubjectFrontPageData}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends MockitoSugar
    with DataSource
    with SubjectPageRepository
    with FrontPageRepository
    with FilmFrontPageRepository
    with FilmPageController
    with SubjectPageController
    with FrontPageController
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
    with Routes[Eff] {
  override val props = new FrontpageApiProperties

  override val clock      = mock[SystemClock]
  override val migrator   = mock[DBMigrator]
  override val dataSource = mock[HikariDataSource]

  override val filmPageController      = mock[FilmPageController]
  override val subjectPageController   = mock[SubjectPageController]
  override val frontPageController     = mock[FrontPageController]
  override val subjectPageRepository   = mock[SubjectPageRepository]
  override val frontPageRepository     = mock[FrontPageRepository]
  override val filmFrontPageRepository = mock[FilmFrontPageRepository]
  override val readService             = mock[ReadService]
  override val writeService            = mock[WriteService]

  def services: List[Service[Eff]] = List.empty
}

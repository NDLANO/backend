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
import no.ndla.frontpageapi.controller.{FilmPageController, FrontPageController, SubjectPageController}
import no.ndla.frontpageapi.model.api.ErrorHandling
import no.ndla.frontpageapi.model.domain.{DBFilmFrontPage, DBFrontPage, DBSubjectPage}
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.network.tapir.TapirApplication
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with MockitoSugar
    with DataSource
    with SubjectPageRepository
    with FrontPageRepository
    with FilmFrontPageRepository
    with FilmPageController
    with SubjectPageController
    with FrontPageController
    with ReadService
    with WriteService
    with ConverterService
    with Props
    with DBFilmFrontPage
    with DBSubjectPage
    with DBFrontPage
    with ErrorHandling
    with Clock
    with DBMigrator {
  override lazy val props = new FrontpageApiProperties

  override lazy val clock: SystemClock           = mock[SystemClock]
  override lazy val migrator: DBMigrator         = mock[DBMigrator]
  override lazy val dataSource: HikariDataSource = mock[HikariDataSource]

  override lazy val filmPageController: FilmPageController           = mock[FilmPageController]
  override lazy val subjectPageController: SubjectPageController     = mock[SubjectPageController]
  override lazy val frontPageController: FrontPageController         = mock[FrontPageController]
  override lazy val subjectPageRepository: SubjectPageRepository     = mock[SubjectPageRepository]
  override lazy val frontPageRepository: FrontPageRepository         = mock[FrontPageRepository]
  override lazy val filmFrontPageRepository: FilmFrontPageRepository = mock[FilmFrontPageRepository]
  override lazy val healthController: TapirHealthController          = mock[TapirHealthController]
  override lazy val readService: ReadService                         = mock[ReadService]
  override lazy val writeService: WriteService                       = mock[WriteService]

  override lazy val ndlaClient: NdlaClient           = mock[NdlaClient]
  override lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}

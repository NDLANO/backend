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

trait TestEnvironment extends TapirApplication with MockitoSugar {
  given props = new FrontpageApiProperties

  given clock: SystemClock           = mock[SystemClock]
  given migrator: DBMigrator         = mock[DBMigrator]
  given dataSource: HikariDataSource = mock[HikariDataSource]

  given filmPageController: FilmPageController           = mock[FilmPageController]
  given subjectPageController: SubjectPageController     = mock[SubjectPageController]
  given frontPageController: FrontPageController         = mock[FrontPageController]
  given subjectPageRepository: SubjectPageRepository     = mock[SubjectPageRepository]
  given frontPageRepository: FrontPageRepository         = mock[FrontPageRepository]
  given filmFrontPageRepository: FilmFrontPageRepository = mock[FilmFrontPageRepository]
  given healthController: TapirHealthController          = mock[TapirHealthController]
  given readService: ReadService                         = mock[ReadService]
  given writeService: WriteService                       = mock[WriteService]

  given ndlaClient: NdlaClient           = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}

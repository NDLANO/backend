/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.IO
import no.ndla.frontpageapi.controller.{FilmPageController, InternController}
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.repository.{FilmFrontPageRepository, FrontPageRepository, SubjectPageRepository}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends MockitoSugar
    with DataSource
    with SubjectPageRepository
    with FrontPageRepository
    with FilmFrontPageRepository
    with FilmPageController
    with ReadService
    with WriteService {

  override val dataSource = mock[javax.sql.DataSource]

  override val filmPageController = mock[FilmPageController[IO]]
  override val subjectPageRepository = mock[SubjectPageRepository]
  override val frontPageRepository = mock[FrontPageRepository]
  override val filmFrontPageRepository = mock[FilmFrontPageRepository]
  override val readService = mock[ReadService]
  override val writeService = mock[WriteService]
}

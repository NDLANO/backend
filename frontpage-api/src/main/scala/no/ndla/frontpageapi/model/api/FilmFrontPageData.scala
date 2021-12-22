/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.auto._

case class FilmFrontPageData(name: String,
                             about: Seq[AboutFilmSubject],
                             movieThemes: Seq[MovieTheme],
                             slideShow: Seq[String])

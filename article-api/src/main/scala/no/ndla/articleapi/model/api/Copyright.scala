/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime

@description("Description of copyright information")
case class Copyright(
    @description("Describes the license of the article") license: License,
    @description("Reference to where the article is procured") origin: String,
    @description("List of creators") creators: Seq[Author],
    @description("List of processors") processors: Seq[Author],
    @description("List of rightsholders") rightsholders: Seq[Author],
    @description("Reference to agreement id") agreementId: Option[Long],
    @description("Date from which the copyright is valid") validFrom: Option[LocalDateTime],
    @description("Date to which the copyright is valid") validTo: Option[LocalDateTime]
)

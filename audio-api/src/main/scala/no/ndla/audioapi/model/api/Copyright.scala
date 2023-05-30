/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime

@description("Description of copyright information")
case class Copyright(
    @description("The license for the audio") license: License,
    @description("Reference to where the audio is procured") origin: Option[String],
    @description("List of creators") creators: Seq[Author] = Seq.empty,
    @description("List of processors") processors: Seq[Author] = Seq.empty,
    @description("List of rightsholders") rightsholders: Seq[Author] = Seq.empty,
    @description("Reference to a agreement id") agreementId: Option[Long],
    @description("Date from which the copyright is valid") validFrom: Option[LocalDateTime],
    @description("Date to which the copyright is valid") validTo: Option[LocalDateTime]
)

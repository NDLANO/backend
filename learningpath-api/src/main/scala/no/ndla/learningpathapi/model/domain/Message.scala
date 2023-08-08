/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.model.NDLADate

case class Message(message: String, adminName: String, date: NDLADate)

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Id for a single Article")
case class ContentId(@description("The unique id of the article") id: Long)

/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import sttp.tapir.Schema.annotations.description

@description("Description of license information")
case class License(
    @description("The name of the license") license: String,
    @description("Description of the license") description: Option[String],
    @description("Url to where the license can be found") url: Option[String]
)

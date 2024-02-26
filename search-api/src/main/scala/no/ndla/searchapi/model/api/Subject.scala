/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Short summary of information about the subject")
case class Subject(
    @description("The name of the subject") name: String,
    @description("The path to the article") path: String,
    @description("List of breadcrumbs to article") breadcrumbs: Seq[String]
)

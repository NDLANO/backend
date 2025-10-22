/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class FootNoteItem(
    title: String,
    `type`: String,
    year: String,
    edition: String,
    publisher: String,
    authors: Seq[String],
)

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

case class SearchableAgreement(
    id: Long,
    title: String,
    content: String,
    license: String
)

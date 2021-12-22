/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class SubjectCollection(name: String, subjects: List[SubjectFilters])
case class SubjectFilters(id: String, filters: List[String])

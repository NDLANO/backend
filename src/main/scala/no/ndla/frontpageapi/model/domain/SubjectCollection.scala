/*
 * Part of NDLA frontpage_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

case class SubjectCollection(name: String, subjects: List[SubjectFilters])
case class SubjectFilters(id: String, filters: List[String])
/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import io.circe._, io.circe.generic.semiauto._

case class SubjectCollection(name: String, subjects: List[SubjectFilters])
case class SubjectFilters(id: String, filters: List[String])

object SubjectCollection {
  implicit val encoder: Encoder[SubjectCollection] = deriveEncoder
  implicit val decoder: Decoder[SubjectCollection] = deriveDecoder
}

object SubjectFilters {
  implicit val encoder: Encoder[SubjectFilters] = deriveEncoder
  implicit val decoder: Decoder[SubjectFilters] = deriveDecoder
}

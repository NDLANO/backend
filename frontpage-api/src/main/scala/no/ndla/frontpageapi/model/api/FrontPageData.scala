/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import io.circe._, io.circe.generic.semiauto._

case class FrontPageData(topical: List[String], categories: List[SubjectCollection])

object FrontPageData {
  implicit val encoder: Encoder[FrontPageData] = deriveEncoder[no.ndla.frontpageapi.model.api.FrontPageData]
  implicit val decoder: Decoder[FrontPageData] = deriveDecoder[no.ndla.frontpageapi.model.api.FrontPageData]
}

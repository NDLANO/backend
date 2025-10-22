/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.myndla

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class MyNDLAGroup(id: String, displayName: String, isPrimarySchool: Boolean, parentId: Option[String])

object MyNDLAGroup {
  implicit val encoder: Encoder[MyNDLAGroup] = deriveEncoder
  implicit val decoder: Decoder[MyNDLAGroup] = deriveDecoder
}

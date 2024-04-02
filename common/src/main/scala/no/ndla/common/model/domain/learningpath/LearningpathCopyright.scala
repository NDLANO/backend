/*
 * Part of NDLA common
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.Author

case class LearningpathCopyright(license: String, contributors: Seq[Author])

object LearningpathCopyright {
  implicit val encoder: Encoder[LearningpathCopyright] = deriveEncoder
  implicit val decoder: Decoder[LearningpathCopyright] = deriveDecoder
}

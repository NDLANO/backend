/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

sealed trait TagOutput
case class SomeStringList(list: List[String])      extends TagOutput
case class SomeTagList(list: List[SubjectTagsDTO]) extends TagOutput

@description("A subject id, and list of tags used in the subject")
case class SubjectTagsDTO(
    @description("Taxonomy id of the subject") subjectId: String,
    @description("List of tags used in the subject") tags: List[String],
    @description("Language for the specified tags") language: String
)

object SubjectTagsDTO {
  implicit val encoder: Encoder[SubjectTagsDTO] = deriveEncoder
  implicit val decoder: Decoder[SubjectTagsDTO] = deriveDecoder
}

/*
 * Part of NDLA concept-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import io.circe.{Decoder, Encoder}
import no.ndla.conceptapi.model.api.InvalidStatusException

import scala.util.{Failure, Success, Try}

object ConceptType extends Enumeration {
  val CONCEPT: ConceptType.Value = Value("concept")
  val GLOSS: ConceptType.Value   = Value("gloss")

  implicit val encoder: Encoder[ConceptType.Value] = Encoder.encodeEnumeration(ConceptType)
  implicit val decoder: Decoder[ConceptType.Value] = Decoder.decodeEnumeration(ConceptType)

  def all: Seq[String]                                      = ConceptType.values.map(_.toString).toSeq
  def valueOf(s: String): Option[ConceptType.Value]         = ConceptType.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[ConceptType.Value] = s.flatMap(valueOf)

  def valueOfOrError(s: String): Try[ConceptType.Value] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid concept type. Valid options are ${all.mkString(", ")}."))
      case Some(conceptType) => Success(conceptType)
    }
  }
}

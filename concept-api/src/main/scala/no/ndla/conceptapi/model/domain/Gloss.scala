/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.conceptapi.model.api.InvalidStatusException
import scala.util.{Failure, Success, Try}

object GlossType extends Enumeration {
  val ADJECTIVE: GlossType.Value    = Value("adjective")
  val ADVERB: GlossType.Value       = Value("adverb")
  val ARTICLE: GlossType.Value      = Value("article")
  val CONJUNCTION: GlossType.Value  = Value("conjunction")
  val DETERMINER: GlossType.Value   = Value("determiner")
  val INTERJECTION: GlossType.Value = Value("interjection")
  val NOUN: GlossType.Value         = Value("noun")
  val NUMERAL: GlossType.Value      = Value("numeral")
  val PREPOSITION: GlossType.Value  = Value("preposition")
  val PRONOUN: GlossType.Value      = Value("pronoun")
  val VERB: GlossType.Value         = Value("verb")

  def all: Seq[String]                                    = GlossType.values.map(_.toString).toSeq
  def valueOf(s: String): Option[GlossType.Value]         = GlossType.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[GlossType.Value] = s.flatMap(valueOf)

  def valueOfOrError(s: String): Try[GlossType.Value] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid gloss type. Valid options are ${all.mkString(", ")}."))
      case Some(conceptType) => Success(conceptType)
    }
  }
}

case class GlossExample(example: String, language: String)
case class GlossData(glossType: GlossType.Value, originalLanguage: String, examples: List[List[GlossExample]])

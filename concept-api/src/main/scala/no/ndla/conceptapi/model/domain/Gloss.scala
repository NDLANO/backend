/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.conceptapi.model.api.InvalidStatusException
import scala.util.{Failure, Success, Try}

object WordClass extends Enumeration {
  val ADJECTIVE: WordClass.Value    = Value("adjective")
  val ADVERB: WordClass.Value       = Value("adverb")
  val ARTICLE: WordClass.Value      = Value("article")
  val CONJUNCTION: WordClass.Value  = Value("conjunction")
  val DETERMINER: WordClass.Value   = Value("determiner")
  val INTERJECTION: WordClass.Value = Value("interjection")
  val NOUN: WordClass.Value         = Value("noun")
  val NUMERAL: WordClass.Value      = Value("numeral")
  val PREPOSITION: WordClass.Value  = Value("preposition")
  val PRONOUN: WordClass.Value      = Value("pronoun")
  val VERB: WordClass.Value         = Value("verb")

  def all: Seq[String]                                    = WordClass.values.map(_.toString).toSeq
  def valueOf(s: String): Option[WordClass.Value]         = WordClass.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[WordClass.Value] = s.flatMap(valueOf)

  def valueOfOrError(s: String): Try[WordClass.Value] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid gloss type. Valid options are ${all.mkString(", ")}."))
      case Some(conceptType) => Success(conceptType)
    }
  }
}

case class GlossExample(example: String, language: String)
case class GlossData(
    wordClass: WordClass.Value,
    originalLanguage: String,
    transcriptions: Map[String, String],
    examples: List[List[GlossExample]]
)

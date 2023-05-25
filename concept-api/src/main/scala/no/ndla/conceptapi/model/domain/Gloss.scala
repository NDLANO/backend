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

  // Part of speech of european languages
  val ADJECTIVE: WordClass.Value    = Value("adjective")
  val ADVERB: WordClass.Value       = Value("adverb")
  val ARTICLE: WordClass.Value      = Value("article")
  val CONJUNCTION: WordClass.Value  = Value("conjunction")
  val DETERMINER: WordClass.Value   = Value("determiner")
  val EXPRESSION: WordClass.Value   = Value("expression")
  val INTERJECTION: WordClass.Value = Value("interjection")
  val NOUN: WordClass.Value         = Value("noun")
  val NUMERAL: WordClass.Value      = Value("numeral")
  val PREPOSITION: WordClass.Value  = Value("preposition")
  val PRONOUN: WordClass.Value      = Value("pronoun")
  val SUBJUNCTION: WordClass.Value  = Value("subjunction")
  val VERB: WordClass.Value         = Value("verb")

  // Part of speech of the chinese language
  val AUXILIARY: WordClass.Value        = Value("auxiliary")
  val COMPLEMENT: WordClass.Value       = Value("complement")
  val CONDITION: WordClass.Value        = Value("condition")
  val COVERB: WordClass.Value           = Value("coverb")
  val CUE: WordClass.Value              = Value("cue")
  val EXCLAMATION: WordClass.Value      = Value("exclamation")
  val INTERROGATIVE: WordClass.Value    = Value("interrogative")
  val MARKER: WordClass.Value           = Value("marker")
  val MODAL_VERB: WordClass.Value       = Value("modal-verb")
  val NOMEN: WordClass.Value            = Value("nomen")
  val NOMINAL: WordClass.Value          = Value("nominal")
  val NUMBER: WordClass.Value           = Value("number")
  val OBJECT: WordClass.Value           = Value("object")
  val ONOMATOPOEIA: WordClass.Value     = Value("onomatopoeia")
  val PARTICLE: WordClass.Value         = Value("particle")
  val PERSONAL_PRONOUN: WordClass.Value = Value("personal-pronoun")
  val PROPER_NOUN: WordClass.Value      = Value("proper-noun")
  val QUANTIFIER: WordClass.Value       = Value("quantifier")
  val SUFFIX: WordClass.Value           = Value("suffix")
  val TIME: WordClass.Value             = Value("time")
  val TIME_EXPRESSION: WordClass.Value  = Value("time-expression")
  val TARGET: WordClass.Value           = Value("target")

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

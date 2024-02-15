/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.conceptapi.model.api.InvalidStatusException

import scala.util.{Failure, Success, Try}

object WordClass extends Enumeration {

  // Part of speech of european languages
  val ADJECTIVE: WordClass.Value                 = Value("adjective")
  val ADVERB: WordClass.Value                    = Value("adverb")
  val CONJUNCTION: WordClass.Value               = Value("conjunction")
  val DETERMINER: WordClass.Value                = Value("determiner")
  val EXPRESSION: WordClass.Value                = Value("expression")
  val INTERJECTION: WordClass.Value              = Value("interjection")
  val NOUN: WordClass.Value                      = Value("noun")
  val PREPOSITION: WordClass.Value               = Value("preposition")
  val PRONOUN: WordClass.Value                   = Value("pronoun")
  val SUBORDINATING_CONJUNCTION: WordClass.Value = Value("subordinating-conjunction")
  val VERB: WordClass.Value                      = Value("verb")

  // Part of speech of the chinese language
  val AUXILIARY: WordClass.Value        = Value("auxiliary")
  val COMPLEMENT: WordClass.Value       = Value("complement")
  val COVERB: WordClass.Value           = Value("coverb")
  val DEMONSTRATIVE: WordClass.Value    = Value("demonstrative")
  val EXCLAMATION_WORD: WordClass.Value = Value("exclamation-word")
  val LOCATION_WORD: WordClass.Value    = Value("location-word")
  val MEASURE_WORD: WordClass.Value     = Value("measure-word")
  val MARKER: WordClass.Value           = Value("marker")
  val MODAL_VERB: WordClass.Value       = Value("modal-verb")
  val NOUN_PHRASE: WordClass.Value      = Value("noun-phrase")
  val NOUN_ZH: WordClass.Value          = Value("noun-zh")
  val NUMERAL: WordClass.Value          = Value("numeral")
  val ONOMATOPOEIA: WordClass.Value     = Value("onomatopoeia")
  val PARTICLE: WordClass.Value         = Value("particle")
  val PERSONAL_PRONOUN: WordClass.Value = Value("personal-pronoun")
  val PROPER_NOUN: WordClass.Value      = Value("proper-noun")
  val QUANTIFIER: WordClass.Value       = Value("quantifier")
  val QUESTION_WORD: WordClass.Value    = Value("question-word")
  val STATIVE_VERB: WordClass.Value     = Value("stative-verb")
  val SUFFIX: WordClass.Value           = Value("suffix")
  val TIME_WORD: WordClass.Value        = Value("time-word")
  val TIME_EXPRESSION: WordClass.Value  = Value("time-expression")
  val VERB_COMPLEMENT: WordClass.Value  = Value("verb-complement")
  val VERB_OBJECT: WordClass.Value      = Value("verb-object")

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

  implicit val encoder: Encoder[WordClass.Value] = Encoder.encodeEnumeration(WordClass)
  implicit val decoder: Decoder[WordClass.Value] = Decoder.decodeEnumeration(WordClass)
}

case class GlossExample(example: String, language: String, transcriptions: Map[String, String])

object GlossExample {
  implicit val encoder: Encoder[GlossExample] = deriveEncoder
  implicit val decoder: Decoder[GlossExample] = deriveDecoder
}

case class GlossData(
    gloss: String,
    wordClass: WordClass.Value,
    originalLanguage: String,
    transcriptions: Map[String, String],
    examples: List[List[GlossExample]]
)

object GlossData {
  implicit val encoder: Encoder[GlossData] = deriveEncoder
  implicit val decoder: Decoder[GlossData] = deriveDecoder
}

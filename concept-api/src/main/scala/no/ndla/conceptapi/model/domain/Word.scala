/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.conceptapi.model.api.InvalidStatusException
import scala.util.{Failure, Success, Try}

object WordType extends Enumeration {
  val ADJECTIVE: WordType.Value    = Value("adjective")
  val ADVERB: WordType.Value       = Value("adverb")
  val ARTICLE: WordType.Value      = Value("article")
  val CONJUNCTION: WordType.Value  = Value("conjunction")
  val DETERMINER: WordType.Value   = Value("determiner")
  val INTERJECTION: WordType.Value = Value("interjection")
  val NOUN: WordType.Value         = Value("noun")
  val NUMERAL: WordType.Value      = Value("numeral")
  val PREPOSITION: WordType.Value  = Value("preposition")
  val PRONOUN: WordType.Value      = Value("pronoun")
  val VERB: WordType.Value         = Value("verb")

  def all: Seq[String]                                   = WordType.values.map(_.toString).toSeq
  def valueOf(s: String): Option[WordType.Value]         = WordType.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[WordType.Value] = s.flatMap(valueOf)

  def valueOfOrError(s: String): Try[WordType.Value] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid word type. Valid options are ${all.mkString(", ")}."))
      case Some(conceptType) => Success(conceptType)
    }
  }
}

case class WordExample(example: String, language: String)
case class WordList(wordType: WordType.Value, originalLanguage: String, examples: List[List[WordExample]])

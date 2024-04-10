/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.concept

import com.scalatsi.TypescriptType.TSEnum
import com.scalatsi.{TSNamedType, TSType}
import enumeratum.*
import no.ndla.common.errors.InvalidStatusException

import scala.util.{Failure, Success, Try}

sealed abstract class WordClass(override val entryName: String) extends EnumEntry

object WordClass extends Enum[WordClass] with CirceEnum[WordClass] {
  override def values: IndexedSeq[WordClass] = findValues

  // Part of speech of european languages
  case object ADJECTIVE                 extends WordClass("adjective")
  case object ADVERB                    extends WordClass("adverb")
  case object CONJUNCTION               extends WordClass("conjunction")
  case object DETERMINER                extends WordClass("determiner")
  case object EXPRESSION                extends WordClass("expression")
  case object INTERJECTION              extends WordClass("interjection")
  case object NOUN                      extends WordClass("noun")
  case object PREPOSITION               extends WordClass("preposition")
  case object PRONOUN                   extends WordClass("pronoun")
  case object SUBORDINATING_CONJUNCTION extends WordClass("subordinating-conjunction")
  case object VERB                      extends WordClass("verb")

  // Part of speech of the chinese language
  case object AUXILIARY        extends WordClass("auxiliary")
  case object COMPLEMENT       extends WordClass("complement")
  case object COVERB           extends WordClass("coverb")
  case object DEMONSTRATIVE    extends WordClass("demonstrative")
  case object EXCLAMATION_WORD extends WordClass("exclamation-word")
  case object LOCATION_WORD    extends WordClass("location-word")
  case object MEASURE_WORD     extends WordClass("measure-word")
  case object MARKER           extends WordClass("marker")
  case object MODAL_VERB       extends WordClass("modal-verb")
  case object NOUN_PHRASE      extends WordClass("noun-phrase")
  case object NOUN_ZH          extends WordClass("noun-zh")
  case object NUMERAL          extends WordClass("numeral")
  case object ONOMATOPOEIA     extends WordClass("onomatopoeia")
  case object PARTICLE         extends WordClass("particle")
  case object PERSONAL_PRONOUN extends WordClass("personal-pronoun")
  case object PROPER_NOUN      extends WordClass("proper-noun")
  case object QUANTIFIER       extends WordClass("quantifier")
  case object QUESTION_WORD    extends WordClass("question-word")
  case object STATIVE_VERB     extends WordClass("stative-verb")
  case object SUFFIX           extends WordClass("suffix")
  case object TIME_WORD        extends WordClass("time-word")
  case object TIME_EXPRESSION  extends WordClass("time-expression")
  case object VERB_COMPLEMENT  extends WordClass("verb-complement")
  case object VERB_OBJECT      extends WordClass("verb-object")

  def all: Seq[String]                              = WordClass.values.map(_.entryName).toSeq
  def valueOf(s: String): Option[WordClass]         = WordClass.withNameOption(s)
  def valueOf(s: Option[String]): Option[WordClass] = s.flatMap(valueOf)

  def valueOfOrError(s: String): Try[WordClass] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid gloss type. Valid options are ${all.mkString(", ")}."))
      case Some(conceptType) => Success(conceptType)
    }
  }
  private val tsEnumValues: Seq[(String, String)] = values.map(e => e.toString -> e.entryName)
  implicit val enumTsType: TSNamedType[WordClass] = TSType.alias[WordClass](
    "WordClass",
    TSEnum.string("WordClassEnum", tsEnumValues: _*)
  )
}

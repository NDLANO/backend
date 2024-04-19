/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.concept

import enumeratum.*
import no.ndla.common.CirceUtil.CirceEnumWithErrors
import no.ndla.common.errors.InvalidStatusException

import scala.util.{Failure, Success, Try}

sealed abstract class ConceptType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object ConceptType extends Enum[ConceptType] with CirceEnumWithErrors[ConceptType] {
  case object CONCEPT extends ConceptType("concept")
  case object GLOSS   extends ConceptType("gloss")

  def all: Seq[String]                                = ConceptType.values.map(_.toString)
  def valueOf(s: String): Option[ConceptType]         = ConceptType.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[ConceptType] = s.flatMap(valueOf)

  def valueOfOrError(s: String): Try[ConceptType] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid concept type. Valid options are ${all.mkString(", ")}."))
      case Some(conceptType) => Success(conceptType)
    }
  }

  override def values: IndexedSeq[ConceptType] = findValues
}

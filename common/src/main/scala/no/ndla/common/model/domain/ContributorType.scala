/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import com.scalatsi.{TSNamedType, TSType}
import com.scalatsi.TypescriptType.TSEnum
import enumeratum.*
import no.ndla.common.errors.ValidationException
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class ContributorType(override val entryName: String) extends EnumEntry

object ContributorType extends Enum[ContributorType] with CirceEnum[ContributorType] {
  case object Artist       extends ContributorType("artist")
  case object CoWriter     extends ContributorType("cowriter")
  case object Compiler     extends ContributorType("compiler")
  case object Composer     extends ContributorType("composer")
  case object Correction   extends ContributorType("correction")
  case object Director     extends ContributorType("director")
  case object Distributor  extends ContributorType("distributor")
  case object Editorial    extends ContributorType("editorial")
  case object Facilitator  extends ContributorType("facilitator")
  case object Idea         extends ContributorType("idea")
  case object Illustrator  extends ContributorType("illustrator")
  case object Linguistic   extends ContributorType("linguistic")
  case object Originator   extends ContributorType("originator")
  case object Photographer extends ContributorType("photographer")
  case object Processor    extends ContributorType("processor")
  case object Publisher    extends ContributorType("publisher")
  case object Reader       extends ContributorType("reader")
  case object RightsHolder extends ContributorType("rightsholder")
  case object ScriptWriter extends ContributorType("scriptwriter")
  case object Supplier     extends ContributorType("supplier")
  case object Translator   extends ContributorType("translator")
  case object Writer       extends ContributorType("writer")

  override def values: IndexedSeq[ContributorType] = findValues

  def all: Seq[String]                            = ContributorType.values.map(_.entryName)
  def valueOf(s: String): Option[ContributorType] = ContributorType.withNameOption(s)

  def valueOfOrError(s: String): ContributorType =
    valueOf(s).getOrElse(
      throw ValidationException(
        "articleType",
        s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."
      )
    )

  // TODO: Remove when all data are converted
  val mapping: Map[String, ContributorType] = Map(
    "bearbeider"       -> ContributorType.Processor,
    "distributør"      -> ContributorType.Distributor,
    "forfatter"        -> ContributorType.Writer,
    "forlag"           -> ContributorType.Publisher,
    "fotograf"         -> ContributorType.Photographer,
    "ide"              -> ContributorType.Idea,
    "illustratør"      -> ContributorType.Illustrator,
    "innleser"         -> ContributorType.Reader,
    "komponist"        -> ContributorType.Composer,
    "korrektur"        -> ContributorType.Correction,
    "kunstner"         -> ContributorType.Artist,
    "leverandør"       -> ContributorType.Supplier,
    "manusforfatter"   -> ContributorType.ScriptWriter,
    "medforfatter"     -> ContributorType.CoWriter,
    "opphaver"         -> ContributorType.Originator,
    "opphavsmann"      -> ContributorType.Originator,
    "oversetter"       -> ContributorType.Translator,
    "redaksjonelt"     -> ContributorType.Editorial,
    "regissør"         -> ContributorType.Director,
    "rettighetsholder" -> ContributorType.RightsHolder,
    "sammenstiller"    -> ContributorType.Compiler,
    "språklig"         -> ContributorType.Linguistic,
    "tilrettelegger"   -> ContributorType.Facilitator
  ).withDefaultValue(ContributorType.Writer)

  implicit def schema: Schema[ContributorType]    = schemaForEnumEntry[ContributorType]
  private val tsEnumValues: Seq[(String, String)] = values.map(e => e.toString -> e.entryName)
  implicit val enumTsType: TSNamedType[ContributorType] =
    TSType.alias[ContributorType]("ContributorType", TSEnum.string("ContributorTypeEnum", tsEnumValues*))
}

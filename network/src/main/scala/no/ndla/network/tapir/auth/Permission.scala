/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import com.scalatsi.TypescriptType.TSEnum
import com.scalatsi.{TSNamedType, TSType}
import enumeratum._

import scala.collection.immutable.ListMap

sealed abstract class Permission(override val entryName: String) extends EnumEntry {}

object Permission extends Enum[Permission] with CirceEnum[Permission] {
  case object AUDIO_API_WRITE          extends Permission("audio:write")
  case object ARTICLE_API_PUBLISH      extends Permission("articles:publish")
  case object ARTICLE_API_WRITE        extends Permission("articles:write")
  case object CONCEPT_API_ADMIN        extends Permission("concept:admin")
  case object CONCEPT_API_WRITE        extends Permission("concept:write")
  case object DRAFT_API_ADMIN          extends Permission("drafts:admin")
  case object DRAFT_API_HTML           extends Permission("drafts:html")
  case object DRAFT_API_PUBLISH        extends Permission("drafts:publish")
  case object DRAFT_API_WRITE          extends Permission("drafts:write")
  case object FRONTPAGE_API_ADMIN      extends Permission("frontpage:admin")
  case object FRONTPAGE_API_WRITE      extends Permission("frontpage:write")
  case object IMAGE_API_WRITE          extends Permission("images:write")
  case object LEARNINGPATH_API_ADMIN   extends Permission("learningpath:admin")
  case object LEARNINGPATH_API_PUBLISH extends Permission("learningpath:publish")
  case object LEARNINGPATH_API_WRITE   extends Permission("learningpath:write")

  override def values: IndexedSeq[Permission] = findValues

  def fromString(s: String): Option[Permission]     = values.find(_.entryName == s)
  def fromStrings(s: List[String]): Set[Permission] = s.flatMap(fromString).toSet

  def thatStartsWith(start: String): List[Permission] = values.filter(_.entryName.startsWith(start)).toList
  def toSwaggerMap(scopes: List[Permission]): ListMap[String, String] =
    ListMap.from(scopes.map(s => s.entryName -> s.entryName))

  private val tsEnumValues: Seq[(String, String)] = values.map(e => e.toString -> e.entryName)
  implicit val enumTsType: TSNamedType[Permission] = TSType.alias[Permission](
    "Permission",
    TSEnum.string("PermissionEnum", tsEnumValues: _*)
  )
}

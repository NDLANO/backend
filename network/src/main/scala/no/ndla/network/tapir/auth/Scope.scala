/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir.auth

import enumeratum._
import scala.collection.immutable.ListMap

sealed abstract class Scope(override val entryName: String) extends EnumEntry {}

object Scope extends Enum[Scope] {
  case object AUDIO_API_WRITE          extends Scope("audio:write")
  case object ARTICLE_API_PUBLISH      extends Scope("articles:publish")
  case object ARTICLE_API_WRITE        extends Scope("articles:write")
  case object CONCEPT_API_ADMIN        extends Scope("concept:admin")
  case object CONCEPT_API_WRITE        extends Scope("concept:write")
  case object DRAFT_API_ADMIN          extends Scope("drafts:admin")
  case object DRAFT_API_HTML           extends Scope("drafts:html")
  case object DRAFT_API_PUBLISH        extends Scope("drafts:publish")
  case object DRAFT_API_WRITE          extends Scope("drafts:write")
  case object FRONTPAGE_API_WRITE      extends Scope("frontpage:write")
  case object IMAGE_API_WRITE          extends Scope("images:write")
  case object LEARNINGPATH_API_ADMIN   extends Scope("learningpath:admin")
  case object LEARNINGPATH_API_PUBLISH extends Scope("learningpath:publish")
  case object LEARNINGPATH_API_WRITE   extends Scope("learningpath:write")

  override def values: IndexedSeq[Scope] = findValues

  def fromString(s: String): Option[Scope]     = values.find(_.entryName == s)
  def fromStrings(s: List[String]): Set[Scope] = s.flatMap(fromString).toSet

  def thatStartsWith(start: String): List[Scope] = values.filter(_.entryName.startsWith(start)).toList
  def toSwaggerMap(scopes: List[Scope]): ListMap[String, String] =
    ListMap.from(scopes.map(s => s.entryName -> s.entryName))
}

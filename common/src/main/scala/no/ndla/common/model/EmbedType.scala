/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model

import enumeratum.*
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.schemaForEnumEntry

sealed abstract class EmbedType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object EmbedType extends Enum[EmbedType] with CirceEnum[EmbedType] {
  val values: IndexedSeq[EmbedType]      = findValues
  implicit val schema: Schema[EmbedType] = schemaForEnumEntry[EmbedType]

  def valueOf(s: String): Option[EmbedType] = EmbedType.values.find(_.entryName == s)

  case object Audio           extends EmbedType("audio")
  case object Brightcove      extends EmbedType("brightcove")
  case object CampaignBlock   extends EmbedType("campaign-block")
  case object CodeBlock       extends EmbedType("code-block")
  case object Comment         extends EmbedType("comment")
  case object Concept         extends EmbedType("concept")
  case object ContactBlock    extends EmbedType("contact-block")
  case object ContentLink     extends EmbedType("content-link")
  case object Copyright       extends EmbedType("copyright")
  case object Error           extends EmbedType("error")
  case object ExternalContent extends EmbedType("external")
  case object File            extends EmbedType("file")
  case object FootNote        extends EmbedType("footnote")
  case object H5P             extends EmbedType("h5p")
  case object IframeContent   extends EmbedType("iframe")
  case object Image           extends EmbedType("image")
  case object KeyFigure       extends EmbedType("key-figure")
  case object LinkBlock       extends EmbedType("link-block")
  case object NRKContent      extends EmbedType("nrk")
  case object Pitch           extends EmbedType("pitch")
  case object RelatedContent  extends EmbedType("related-content")
  case object Symbol          extends EmbedType("symbol")
  case object UuDisclaimer    extends EmbedType("uu-disclaimer")

}

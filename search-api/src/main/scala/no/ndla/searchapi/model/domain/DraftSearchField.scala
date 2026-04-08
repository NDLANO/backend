/*
 * Part of NDLA search-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class DraftSearchField(override val entryName: String, val alwaysInclude: Boolean) extends EnumEntry

object DraftSearchField extends Enum[DraftSearchField] with CirceEnum[DraftSearchField] {

  val values: IndexedSeq[DraftSearchField] = findValues

  case object Title           extends DraftSearchField("title", true)
  case object Introduction    extends DraftSearchField("introduction", true)
  case object MetaDescription extends DraftSearchField("metaDescription", true)
  case object Disclaimer      extends DraftSearchField("disclaimer", true)
  case object Content         extends DraftSearchField("content", true)
  case object Tags            extends DraftSearchField("tags", true)
  case object EmbedAttributes extends DraftSearchField("embedAttributes", true)
  case object Creators        extends DraftSearchField("creators", true)
  case object Processors      extends DraftSearchField("processors", true)
  case object Rightsholders   extends DraftSearchField("rightsholders", true)
  case object RevisionMeta    extends DraftSearchField("revisionMeta", true)
  case object Notes           extends DraftSearchField("notes", false)
  case object PreviousNotes   extends DraftSearchField("previousNotes", false)

  implicit val schema: Schema[DraftSearchField]    = schemaForEnumEntry[DraftSearchField]
  implicit val codec: PlainCodec[DraftSearchField] = plainCodecEnumEntry[DraftSearchField]
}

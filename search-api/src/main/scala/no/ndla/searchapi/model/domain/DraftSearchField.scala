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

sealed abstract class DraftSearchField(override val entryName: String) extends EnumEntry

object DraftSearchField extends Enum[DraftSearchField] with CirceEnum[DraftSearchField] {

  val values: IndexedSeq[DraftSearchField] = findValues

  case object Title           extends DraftSearchField("title")
  case object Introduction    extends DraftSearchField("introduction")
  case object MetaDescription extends DraftSearchField("metaDescription")
  case object Disclaimer      extends DraftSearchField("disclaimer")
  case object Content         extends DraftSearchField("content")
  case object Tags            extends DraftSearchField("tags")
  case object EmbedAttributes extends DraftSearchField("embedAttributes")
  case object Creators        extends DraftSearchField("creators")
  case object Processors      extends DraftSearchField("processors")
  case object Rightsholders   extends DraftSearchField("rightsholders")
  case object RevisionMeta    extends DraftSearchField("revisionMeta")
  case object Notes           extends DraftSearchField("notes")

  implicit val schema: Schema[DraftSearchField]    = schemaForEnumEntry[DraftSearchField]
  implicit val codec: PlainCodec[DraftSearchField] = plainCodecEnumEntry[DraftSearchField]
}

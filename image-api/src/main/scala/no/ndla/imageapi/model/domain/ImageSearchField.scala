/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class ImageSearchField(override val entryName: String) extends EnumEntry

object ImageSearchField extends Enum[ImageSearchField] with CirceEnum[ImageSearchField] {

  val values: IndexedSeq[ImageSearchField] = findValues

  case object Titles        extends ImageSearchField("titles")
  case object Alttexts      extends ImageSearchField("alttexts")
  case object Captions      extends ImageSearchField("captions")
  case object Tags          extends ImageSearchField("tags")
  case object Creators      extends ImageSearchField("creators")
  case object Processors    extends ImageSearchField("processors")
  case object Rightsholders extends ImageSearchField("rightsholders")
  case object EditorNotes   extends ImageSearchField("editorNotes")

  implicit val schema: Schema[ImageSearchField]    = schemaForEnumEntry[ImageSearchField]
  implicit val codec: PlainCodec[ImageSearchField] = plainCodecEnumEntry[ImageSearchField]
}

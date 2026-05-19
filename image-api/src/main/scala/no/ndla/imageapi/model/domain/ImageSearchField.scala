/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{Codec, Schema}

enum ImageSearchField(val entryName: String) {
  case Titles        extends ImageSearchField("titles")
  case Alttexts      extends ImageSearchField("alttexts")
  case Captions      extends ImageSearchField("captions")
  case Tags          extends ImageSearchField("tags")
  case Creators      extends ImageSearchField("creators")
  case Processors    extends ImageSearchField("processors")
  case Rightsholders extends ImageSearchField("rightsholders")
  case EditorNotes   extends ImageSearchField("editorNotes")
}

object ImageSearchField {
  def withNameOption(name: String): Option[ImageSearchField] = values.find(_.entryName == name)

  implicit val schema: Schema[ImageSearchField] =
    Schema.derivedEnumeration[ImageSearchField](encode = Some(_.entryName))

  implicit val codec: PlainCodec[ImageSearchField] =
    Codec.derivedEnumeration[String, ImageSearchField](decode = withNameOption, encode = _.entryName)

  implicit val encoder: Encoder[ImageSearchField] = Encoder.encodeString.contramap(_.entryName)
  implicit val decoder: Decoder[ImageSearchField] = Decoder
    .decodeString
    .emap(s => withNameOption(s).toRight(s"Unknown ImageSearchField: $s"))
}

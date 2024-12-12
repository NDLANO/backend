/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import enumeratum._
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

sealed abstract class CategorySortDTO(override val entryName: String) extends EnumEntry

object CategorySortDTO extends Enum[CategorySortDTO] with CirceEnum[CategorySortDTO] {
  case object ByTitle extends CategorySortDTO("title")
  case object ByRank  extends CategorySortDTO("rank")

  val all: Seq[String] = values.map(_.entryName)

  override def values: IndexedSeq[CategorySortDTO] = findValues

  private def fromOptString(maybeString: Option[String]): Option[CategorySortDTO] = {
    maybeString.flatMap { s => CategorySortDTO.withNameInsensitiveOption(s) }
  }

  implicit val queryParamCodec: Codec[List[String], CategorySortDTO, CodecFormat.TextPlain] = {
    Codec
      .id[List[String], TextPlain](TextPlain(), Schema.string)
      .mapDecode(x => DecodeResult.fromOption(fromOptString(x.headOption)))(x => List(x.entryName))
  }
}

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

sealed abstract class CategorySort(override val entryName: String) extends EnumEntry

object CategorySort extends Enum[CategorySort] with CirceEnum[CategorySort] {
  case object ByTitle extends CategorySort("title")
  case object ByRank  extends CategorySort("rank")

  val all: Seq[String] = values.map(_.entryName)

  override def values: IndexedSeq[CategorySort] = findValues

  private def fromOptString(maybeString: Option[String]): Option[CategorySort] = {
    maybeString.flatMap { s => CategorySort.withNameInsensitiveOption(s) }
  }

  implicit val queryParamCodec: Codec[List[String], CategorySort, CodecFormat.TextPlain] = {
    Codec
      .id[List[String], TextPlain](TextPlain(), Schema.string)
      .mapDecode(x => DecodeResult.fromOption(fromOptString(x.headOption)))(x => List(x.entryName))
  }
}

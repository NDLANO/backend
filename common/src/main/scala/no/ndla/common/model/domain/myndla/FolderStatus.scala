/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.myndla

import no.ndla.common.errors.InvalidStatusException
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

import scala.util.{Failure, Success, Try}

object FolderStatus extends Enumeration {
  val PRIVATE: FolderStatus.Value = Value("private")
  val SHARED: FolderStatus.Value  = Value("shared")

  def all: Seq[String] = FolderStatus.values.map(_.toString).toSeq

  def valueOf(s: String): Option[FolderStatus.Value] = {
    FolderStatus.values.find(_.toString == s)
  }

  def valueOf(s: Option[String]): Option[FolderStatus.Value] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }

  def valueOfOrError(s: String): Try[FolderStatus.Value] = {
    valueOf(s) match {
      case None =>
        Failure(InvalidStatusException(s"'$s' is not a valid folder status. Valid options are ${all.mkString(", ")}."))
      case Some(folderStatus) => Success(folderStatus)
    }
  }

  implicit val schema: Schema[FolderStatus.Value] = Schema.string
  implicit val queryParamCodec: Codec[List[String], Option[FolderStatus.Value], CodecFormat.TextPlain] = {
    Codec
      .id[List[String], TextPlain](TextPlain(), Schema.string)
      .mapDecode(x =>
        DecodeResult.Value(
          valueOf(x.headOption)
        )
      )(x => x.map(_.toString).toList)
  }
}

/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import sttp.model.Part

import java.io.{File, FileInputStream, InputStream}
import scala.util.Try

case class UploadedFile(
    partName: String,
    fileName: Option[String],
    fileSize: Long,
    contentType: Option[String],
    file: File
) {
  def doWithStream[T](f: UploadedFile => Try[T]): Try[T] = {
    try f(this)
    finally file.delete(): Unit
  }

  def stream: InputStream = new FileInputStream(file)
}

object UploadedFile {
  private def stripQuotes(s: String): String           = s.stripPrefix("\"").stripSuffix("\"")
  def fromFilePart(filePart: Part[File]): UploadedFile = {
    val file     = filePart.body
    val partName = stripQuotes(filePart.name)
    val fileName = filePart.fileName.map(stripQuotes)

    new UploadedFile(
      partName = partName,
      fileSize = file.length(),
      contentType = filePart.contentType,
      fileName = fileName,
      file = file
    )
  }
}

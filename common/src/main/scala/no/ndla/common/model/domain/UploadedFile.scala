/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import sttp.model.Part

import java.io.{File, FileInputStream}
import scala.util.Try

case class UploadedFile(
    partName: String,
    stream: FileInputStream,
    fileName: Option[String],
    fileSize: Long,
    contentType: Option[String],
    private val file: File
) {
  def doWithStream[T](f: UploadedFile => Try[T]): Try[T] = {
    try f(this)
    finally file.delete(): Unit
  }
}

object UploadedFile {
  def fromFilePart(filePart: Part[File]): UploadedFile = {
    val file        = filePart.body
    val inputStream = new FileInputStream(file)

    new UploadedFile(
      partName = filePart.name,
      stream = inputStream,
      fileSize = file.length(),
      contentType = filePart.contentType,
      fileName = filePart.fileName,
      file = file
    )
  }
}

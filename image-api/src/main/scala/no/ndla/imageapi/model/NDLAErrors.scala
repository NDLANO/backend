/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model

import no.ndla.imageapi.Props

class ImageNotFoundException(message: String) extends RuntimeException(message)

class ImportException(message: String) extends RuntimeException(message)

case class InvalidUrlException(message: String) extends RuntimeException(message)

class ResultWindowTooLargeException(message: String) extends RuntimeException(message)

case class ImageDeleteException(message: String, exs: Seq[Throwable]) extends RuntimeException(message) {
  exs.foreach(ex => addSuppressed(ex))
}
case class ImageConversionException(message: String) extends RuntimeException(message)
case class ImageCopyException(message: String)       extends RuntimeException(message)

object ImageErrorHelpers {
  def fileTooBigError(using props: Props): String =
    s"The file is too big. Max file size is ${props.MaxImageFileSizeBytes / 1024 / 1024} MiB"
  def WINDOW_TOO_LARGE_DESCRIPTION(using props: Props): String =
    s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
}

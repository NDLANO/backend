/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import no.ndla.draftapi.Props
import no.ndla.network.tapir.ErrorHelpers

class DraftErrorHelpers(using props: Props, errorHelpers: ErrorHelpers) {
  import errorHelpers.*
  val WINDOW_TOO_LARGE_DESCRIPTION: String =
    s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

  val fileTooBigDescription: String =
    s"The file is too big. Max file size is ${props.multipartFileSizeThresholdBytes / 1024 / 1024} MiB"

  class OptimisticLockException(message: String = RESOURCE_OUTDATED_DESCRIPTION)      extends RuntimeException(message)
  class ResultWindowTooLargeException(message: String = WINDOW_TOO_LARGE_DESCRIPTION) extends RuntimeException(message)
}

case class IllegalStatusStateTransition(message: String) extends RuntimeException(message)
case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ArticlePublishException(message: String)    extends RuntimeException(message)
case class ArticleVersioningException(message: String) extends RuntimeException(message)

class ArticleStatusException(message: String)   extends RuntimeException(message)
case class CloneFileException(message: String)  extends RuntimeException(message)
case class H5PException(message: String)        extends RuntimeException(message)
case class GenerateIDException(message: String) extends RuntimeException(message)

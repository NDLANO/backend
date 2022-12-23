/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import java.time.LocalDateTime
import scala.annotation.meta.field
import no.ndla.articleapi.Props
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Information about an error")
case class Error(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
    @(ApiModelProperty @field)(description = "Description of the error") description: String,
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: LocalDateTime = LocalDateTime.now(),
    @(ApiModelProperty @field)(description = "The supported languages for an article") supportedLanguages: Option[
      Seq[String]
    ] = None
)

trait ErrorHelpers {
  this: Props =>

  object ErrorHelpers {
    val GENERIC              = "GENERIC"
    val NOT_FOUND            = "NOT_FOUND"
    val INDEX_MISSING        = "INDEX_MISSING"
    val VALIDATION           = "VALIDATION"
    val RESOURCE_OUTDATED    = "RESOURCE_OUTDATED"
    val ACCESS_DENIED        = "ACCESS DENIED"
    val WINDOW_TOO_LARGE     = "RESULT_WINDOW_TOO_LARGE"
    val DATABASE_UNAVAILABLE = "DATABASE_UNAVAILABLE"
    val ARTICLE_GONE         = "ARTICLE_GONE"

    val VALIDATION_DESCRIPTION = "Validation Error"
    val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."

    val INDEX_MISSING_DESCRIPTION =
      s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${props.ContactEmail} if the error persists."
    val RESOURCE_OUTDATED_DESCRIPTION = "The resource is outdated. Please try fetching before submitting again."

    val WINDOW_TOO_LARGE_DESCRIPTION =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    val DATABASE_UNAVAILABLE_DESCRIPTION = s"Database seems to be unavailable, retrying connection."

    val INVALID_SEARCH_CONTEXT_DESCRIPTION =
      "The search-context specified was not expected. Please create one by searching from page 1."

    val ARTICLE_GONE_DESCRIPTION = "The article you are searching for seems to have vanished 👻"

    val GenericError: Error         = Error(GENERIC, GENERIC_DESCRIPTION)
    val IndexMissingError: Error    = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
    val InvalidSearchContext: Error = Error(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION)

    case class ResultWindowTooLargeException(message: String = WINDOW_TOO_LARGE_DESCRIPTION)
        extends RuntimeException(message)
    case class ArticleGoneException(message: String = ARTICLE_GONE_DESCRIPTION) extends RuntimeException(message)
  }

}

case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ImportException(message: String)                             extends RuntimeException(message)
class ImportExceptions(val message: String, val errors: Seq[Throwable]) extends RuntimeException(message)
class ConfigurationException(message: String)                           extends RuntimeException(message)

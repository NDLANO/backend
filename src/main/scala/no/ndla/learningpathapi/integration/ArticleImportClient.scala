/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.ArticleImportHost
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpRequest}

trait ArticleImportClient {
  this: NdlaClient =>
  val articleImportClient: ArticleImportClient

  class ArticleImportClient extends LazyLogging {
    private val ArticleImportTimeout = 600 * 1000 // 600 seconds
    private val ExternalId = ":external_id"
    private val importArticleEndpoint =
      s"http://$ArticleImportHost/intern/import/$ExternalId"

    def importArticle(externalId: String): Try[ArticleImportStatus] =
      doRequest(
        Http(importArticleEndpoint.replace(ExternalId, externalId))
          .timeout(ArticleImportTimeout, ArticleImportTimeout)
          .method("POST"))

    private def doRequest(httpRequest: HttpRequest): Try[ArticleImportStatus] =
      ndlaClient.fetchWithForwardedAuth[ArticleImportStatus](httpRequest)
  }

}
case class ArticleImportStatus(messages: Seq[String], visitedNodes: Seq[String], articleId: Long)

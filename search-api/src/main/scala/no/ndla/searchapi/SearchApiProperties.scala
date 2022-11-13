/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{AuthUser, Domains}
import no.ndla.searchapi.model.search.SearchType

import scala.util.Properties._

trait Props {
  val props: SearchApiProperties
}

class SearchApiProperties extends LazyLogging {
  def Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")
  def ApplicationName     = "search-api"
  def Auth0LoginEndpoint  = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  def ContactName: String     = propOrElse("CONTACT_NAME", "NDLA")
  def ContactUrl: String      = propOrElse("CONTACT_URL", "https://ndla.no")
  def ContactEmail: String    = propOrElse("CONTACT_EMAIL", "hjelp+api@ndla.no")
  def TermsUrl: String        = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  def CorrelationIdKey    = "correlationID"
  def CorrelationIdHeader = "X-Correlation-ID"

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def DraftApiUrl: String        = s"http://${propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")}"
  def ArticleApiUrl: String      = s"http://${propOrElse("ARTICLE_API_HOST", "article-api.ndla-local")}"
  def LearningpathApiUrl: String = s"http://${propOrElse("LEARNINGPATH_API_HOST", "learningpath-api.ndla-local")}"
  def ImageApiUrl: String        = s"http://${propOrElse("IMAGE_API_HOST", "image-api.ndla-local")}"
  def AudioApiUrl: String        = s"http://${propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")}"
  def ApiGatewayUrl: String      = s"http://${propOrElse("API_GATEWAY_HOST", "api-gateway.ndla-local")}"
  def GrepApiUrl: String         = s"https://${propOrElse("GREP_API_HOST", "data.udir.no")}"

  def SearchServer: String                 = propOrElse("SEARCH_SERVER", "http://search-search-api.ndla-local")
  def RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  def SearchIndexes: Map[SearchType.Value, String] = Map(
    SearchType.Articles      -> propOrElse("ARTICLE_SEARCH_INDEX_NAME", "articles"),
    SearchType.Drafts        -> propOrElse("DRAFT_SEARCH_INDEX_NAME", "drafts"),
    SearchType.LearningPaths -> propOrElse("LEARNINGPATH_SEARCH_INDEX_NAME", "learningpaths")
  )

  def SearchDocuments: Map[SearchType.Value, String] = Map(
    SearchType.Articles      -> "article",
    SearchType.Drafts        -> "draft",
    SearchType.LearningPaths -> "learningpath"
  )

  def DefaultPageSize                            = 10
  def MaxPageSize                                = 10000
  def IndexBulkSize                              = 100
  def ElasticSearchIndexMaxResultWindow          = 10000
  def ElasticSearchScrollKeepAlive               = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def RedisHost: String = propOrElse("REDIS_HOST", "localhost")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

  def ExternalApiUrls: Map[String, String] = Map(
    "article-api"      -> s"$Domain/article-api/v2/articles",
    "draft-api"        -> s"$Domain/draft-api/v1/drafts",
    "learningpath-api" -> s"$Domain/learningpath-api/v2/learningpaths",
    "raw-image"        -> s"$Domain/image-api/raw/id"
  )
}

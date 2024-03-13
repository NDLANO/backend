/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.network.{AuthUser, Domains}
import no.ndla.searchapi.model.search.SearchType

import scala.util.Properties._

trait Props extends HasBaseProps {
  val props: SearchApiProperties
}

class SearchApiProperties extends BaseProps with StrictLogging {
  def ApplicationName            = "search-api"
  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-search-api.ndla-local")

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
  def IndexBulkSize: Int                         = propOrElse("INDEX_BULK_SIZE", "100").toInt
  def ElasticSearchIndexMaxResultWindow          = 10000
  def ElasticSearchScrollKeepAlive               = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def RedisHost: String = propOrElse("REDIS_HOST", "redis")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

  def ExternalApiUrls: Map[String, String] = Map(
    "article-api"      -> s"$Domain/article-api/v2/articles",
    "draft-api"        -> s"$Domain/draft-api/v1/drafts",
    "learningpath-api" -> s"$Domain/learningpath-api/v2/learningpaths",
    "raw-image"        -> s"$Domain/image-api/raw/id"
  )
}

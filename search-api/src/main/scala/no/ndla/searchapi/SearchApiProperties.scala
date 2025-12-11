/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.BaseProps
import no.ndla.common.model.api.search.SearchType
import no.ndla.network.{AuthUser, Domains}

import scala.util.Properties.*
import scala.util.{Failure, Success, Try}

type Props = SearchApiProperties

class SearchApiProperties extends BaseProps with StrictLogging {
  def ApplicationName            = "search-api"
  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-search-api.ndla-local")

  val articleIndexName      = propOrElse("ARTICLE_SEARCH_INDEX_NAME", "articles")
  val draftIndexName        = propOrElse("DRAFT_SEARCH_INDEX_NAME", "drafts")
  val learningpathIndexName = propOrElse("LEARNINGPATH_SEARCH_INDEX_NAME", "learningpaths")
  val conceptIndexName      = propOrElse("DRAFT_CONCEPT_SEARCH_INDEX_NAME", "draftconcepts")
  val grepIndexName         = propOrElse("GREP_SEARCH_INDEX_NAME", "greps")
  val nodeIndexName         = propOrElse("NODE_SEARCH_INDEX_NAME", "nodes")

  def SearchIndex(searchType: SearchType) = searchType match {
    case SearchType.Articles      => articleIndexName
    case SearchType.Drafts        => draftIndexName
    case SearchType.LearningPaths => learningpathIndexName
    case SearchType.Concepts      => conceptIndexName
    case SearchType.Grep          => grepIndexName
    case SearchType.Nodes         => nodeIndexName
  }

  def indexToSearchType(indexName: String): Try[SearchType] = indexName match {
    case `articleIndexName`      => Success(SearchType.Articles)
    case `draftIndexName`        => Success(SearchType.Drafts)
    case `learningpathIndexName` => Success(SearchType.LearningPaths)
    case `conceptIndexName`      => Success(SearchType.Concepts)
    case `grepIndexName`         => Success(SearchType.Grep)
    case `nodeIndexName`         => Success(SearchType.Nodes)
    case _                       => Failure(new IllegalArgumentException(s"Unknown index name: $indexName"))
  }

  final val DefaultPageSize                      = 10
  def MaxPageSize                                = 10000
  def IndexBulkSize: Int                         = propOrElse("INDEX_BULK_SIZE", "100").toInt
  def ElasticSearchIndexMaxResultWindow          = 10000
  def ElasticSearchScrollKeepAlive               = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def RedisHost: String = propOrElse("REDIS_HOST", "redis")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

  def ExternalApiUrls: Map[String, String] = Map(
    "article-api"      -> s"$Domain/article-api/v2/articles",
    "concept-api"      -> s"$Domain/concept-api/v1/drafts",
    "draft-api"        -> s"$Domain/draft-api/v1/drafts",
    "learningpath-api" -> s"$Domain/learningpath-api/v2/learningpaths",
    "raw-image"        -> propOrElse("IMAGE_API_CLOUDFRONT_DOMAIN", s"$Domain/image-api/raw").concat("/id"),
  )
}

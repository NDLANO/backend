/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.common.Environment.prop
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.database.{DatabaseProps, HasDatabaseProps}
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties.*

trait Props extends HasBaseProps with HasDatabaseProps {
  val props: ArticleApiProperties
}

class ArticleApiProperties extends BaseProps with DatabaseProps {

  def ApplicationName            = "article-api"
  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def SearchServer: String                       = propOrElse("SEARCH_SERVER", "http://search-article-api.ndla-local")
  def ArticleSearchIndex: String                 = propOrElse("SEARCH_INDEX_NAME", "articles")
  def ArticleSearchDocument                      = "article"
  def DefaultPageSize                            = 10
  def MaxPageSize                                = 10000
  def IndexBulkSize                              = 200
  def ElasticSearchIndexMaxResultWindow          = 10000
  def ElasticSearchScrollKeepAlive               = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  def MinimumAllowedTags = 3

  def RedisHost: String = propOrElse("REDIS_HOST", "redis")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  def maxConvertionRounds = 5

  private def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def externalApiUrls: Map[String, String] = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image"                 -> s"$Domain/image-api/raw/id",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio",
    ResourceType.File.toString  -> Domain,
    ResourceType.H5P.toString   -> H5PAddress
  )

  def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "sub", "sup")
  def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p", "strong")

  private def H5PAddress: String = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "local"   -> "https://h5p-test.ndla.no",
      "test"    -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  def ndlaFrontendUrl: String = Environment match {
    case "local" => "http://localhost:30017"
    case "prod"  => "https://ndla.no"
    case _       => s"https://$Environment.ndla.no"
  }

  private def BrightcoveAccountId: String = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  private def BrightcovePlayerId: String  = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  def BrightcoveVideoScriptUrl: String =
    s"//players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayerId}_default/index.min.js"
  def H5PResizerScriptUrl = "//h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js"
  def NRKVideoScriptUrl: Seq[String] =
    Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")

  override def MetaMigrationLocation: String      = "no/ndla/articleapi/db/migration"
  override def MetaMigrationTable: Option[String] = Some("schema_version")
}

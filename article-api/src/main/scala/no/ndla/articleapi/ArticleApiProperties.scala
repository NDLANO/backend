/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.common.Environment.{booleanPropOrFalse, prop}
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.common.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties._

trait Props extends HasBaseProps {
  val props: ArticleApiProperties
}

class ArticleApiProperties extends BaseProps {

  def ApplicationName            = "article-api"
  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def MetaUserName: String    = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String    = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String    = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String      = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int           = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String      = prop(PropertyKeys.MetaSchemaKey)
  def MetaMaxConnections: Int = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt

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

  def oldCreatorTypes: List[String] = List(
    "opphavsmann",
    "fotograf",
    "kunstner",
    "forfatter",
    "manusforfatter",
    "innleser",
    "oversetter",
    "regissør",
    "illustratør",
    "medforfatter",
    "komponist"
  )

  def creatorTypes: List[String] = List(
    "originator",
    "photographer",
    "artist",
    "writer",
    "scriptwriter",
    "reader",
    "translator",
    "director",
    "illustrator",
    "cowriter",
    "composer"
  )

  def oldProcessorTypes: List[String] =
    List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  def processorTypes: List[String] =
    List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  def oldRightsholderTypes: List[String] = List("rettighetshaver", "forlag", "distributør", "leverandør")
  def rightsholderTypes: List[String]    = List("rightsholder", "publisher", "distributor", "supplier")

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  def maxConvertionRounds = 5

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def externalApiUrls: Map[String, String] = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image"                 -> s"$Domain/image-api/raw/id",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio",
    ResourceType.File.toString  -> Domain,
    ResourceType.H5P.toString   -> H5PAddress
  )

  def InlineHtmlTags: Set[String] =
    if (booleanPropOrFalse("ALLOW_HTML_IN_TITLE")) Set("code", "em", "span", "strong", "sub", "sup") else Set.empty
  def IntroductionHtmlTags: Set[String] =
    if (booleanPropOrFalse("ALLOW_HTML_IN_TITLE")) InlineHtmlTags ++ Set("br", "p") else Set.empty

  private def H5PAddress: String = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "test"    -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no",
      "ff"      -> "https://h5p-ff.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  def BrightcoveAccountId: String        = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  private def BrightcovePlayerId: String = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  def BrightcoveVideoScriptUrl: String =
    s"//players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayerId}_default/index.min.js"
  def H5PResizerScriptUrl = "//h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js"
  def NRKVideoScriptUrl: Seq[String] =
    Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")
}

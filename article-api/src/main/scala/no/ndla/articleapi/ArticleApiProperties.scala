/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.Environment.prop
import no.ndla.common.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties._

trait Props {
  val props: ArticleApiProperties
}

class ArticleApiProperties extends LazyLogging {
  def IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  def Environment: String      = propOrElse("NDLA_ENVIRONMENT", "local")
  def ApplicationName          = "article-api"
  def Auth0LoginEndpoint       = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"
  def RoleWithWriteAccess      = "articles:write"
  def DraftRoleWithWriteAccess = "drafts:write"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  def ContactName: String     = propOrElse("CONTACT_NAME", "NDLA")
  def ContactUrl: String      = propOrElse("CONTACT_URL", "https://ndla.no")
  def ContactEmail: String    = propOrElse("CONTACT_EMAIL", "hjelp+api@ndla.no")
  def TermsUrl: String        = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  def MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String   = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int        = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String   = prop(PropertyKeys.MetaSchemaKey)
  def MetaMaxConnections   = 10

  def SearchServer: String                 = propOrElse("SEARCH_SERVER", "http://search-article-api.ndla-local")
  def RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  def ArticleSearchIndex: String           = propOrElse("SEARCH_INDEX_NAME", "articles")
  def ArticleSearchDocument                = "article"
  def DefaultPageSize                      = 10
  def MaxPageSize                          = 10000
  def IndexBulkSize                        = 200
  def ElasticSearchIndexMaxResultWindow    = 10000
  def ElasticSearchScrollKeepAlive         = "1m"
  def InitialScrollContextKeywords         = List("0", "initial", "start", "first")

  def CorrelationIdKey             = "correlationID"
  def CorrelationIdHeader          = "X-Correlation-ID"
  def AudioHost: String            = propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")
  def ImageHost: String            = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  def DraftHost: String            = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  def SearchHost: String           = propOrElse("SEARCH_API_HOST", "search-api.ndla-local")
  def ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  def MinimumAllowedTags = 3

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
  def allowedAuthors: List[String]       = creatorTypes ++ processorTypes ++ rightsholderTypes

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

  def H5PAddress: String = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "test"    -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no",
      "ff"      -> "https://h5p-ff.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  def BrightcoveAccountId: String = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  def BrightcovePlayerId: String  = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  def BrightcoveVideoScriptUrl =
    s"//players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayerId}_default/index.min.js"
  def H5PResizerScriptUrl = "//h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js"
  def NRKVideoScriptUrl: Seq[String] =
    Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")
}

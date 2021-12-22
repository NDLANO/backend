/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties._

object ArticleApiProperties extends LazyLogging {
  val IsKubernetes: Boolean = envOrNone("NDLA_IS_KUBERNETES").isDefined

  val Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "article-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"
  val RoleWithWriteAccess = "articles:write"
  val DraftRoleWithWriteAccess = "drafts:write"

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  val ContactName: String = propOrElse("CONTACT_NAME", "NDLA")
  val ContactUrl: String = propOrElse("CONTACT_URL", "ndla.no")
  val ContactEmail: String = propOrElse("CONTACT_EMAIL", "support+api@ndla.no")
  val TermsUrl: String = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  lazy val MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  lazy val MetaServer: String = prop(PropertyKeys.MetaServerKey)
  lazy val MetaPort: Int = prop(PropertyKeys.MetaPortKey).toInt
  lazy val MetaSchema: String = prop(PropertyKeys.MetaSchemaKey)
  val MetaMaxConnections = 10

  val SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-article-api.ndla-local")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val ArticleSearchIndex: String = propOrElse("SEARCH_INDEX_NAME", "articles")
  val ArticleSearchDocument = "article"
  val DefaultPageSize = 10
  val MaxPageSize = 10000
  val IndexBulkSize = 200
  val ElasticSearchIndexMaxResultWindow = 10000
  val ElasticSearchScrollKeepAlive = "1m"
  val InitialScrollContextKeywords = List("0", "initial", "start", "first")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val AudioHost: String = propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")
  val ImageHost: String = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  val DraftHost: String = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  val SearchHost: String = propOrElse("SEARCH_API_HOST", "search-api.ndla-local")
  val ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  val MinimumAllowedTags = 3

  val oldCreatorTypes = List("opphavsmann",
                             "fotograf",
                             "kunstner",
                             "forfatter",
                             "manusforfatter",
                             "innleser",
                             "oversetter",
                             "regissør",
                             "illustratør",
                             "medforfatter",
                             "komponist")

  val creatorTypes = List("originator",
                          "photographer",
                          "artist",
                          "writer",
                          "scriptwriter",
                          "reader",
                          "translator",
                          "director",
                          "illustrator",
                          "cowriter",
                          "composer")

  val oldProcessorTypes =
    List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  val processorTypes = List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  val oldRightsholderTypes = List("rettighetshaver", "forlag", "distributør", "leverandør")
  val rightsholderTypes = List("rightsholder", "publisher", "distributor", "supplier")
  val allowedAuthors: List[String] = creatorTypes ++ processorTypes ++ rightsholderTypes

  // When converting a content node, the converter may run several times over the content to make sure
  // everything is converted. This value defines a maximum number of times the converter runs on a node
  val maxConvertionRounds = 5

  lazy val Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  val externalApiUrls = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image" -> s"$Domain/image-api/raw/id",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio",
    ResourceType.File.toString -> Domain,
    ResourceType.H5P.toString -> H5PAddress
  )

  lazy val H5PAddress = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "test" -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no",
      "ff" -> "https://h5p-ff.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  val BrightcoveAccountId: String = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  val BrightcovePlayerId: String = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  val BrightcoveVideoScriptUrl =
    s"//players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayerId}_default/index.min.js"
  val H5PResizerScriptUrl = "//h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js"
  val NRKVideoScriptUrl = Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    propOrNone(key) match {
      case Some(prop) => prop
      case _          => default
    }
  }
}

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties._

object DraftApiProperties extends LazyLogging {

  val Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "draft-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"
  val DraftRoleWithWriteAccess = "drafts:write"
  val DraftRoleWithPublishAccess = "drafts:publish"
  val ArticleRoleWithPublishAccess = "articles:publish"

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  val ContactName: String = propOrElse("CONTACT_NAME", "NDLA")
  val ContactUrl: String = propOrElse("CONTACT_URL", "ndla.no")
  val ContactEmail: String = propOrElse("CONTACT_EMAIL", "support+api@ndla.no")
  val TermsUrl: String = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  def MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String = prop(PropertyKeys.MetaSchemaKey)
  val MetaMaxConnections = 10

  val resourceHtmlEmbedTag = "embed"
  val ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  lazy val Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  val externalApiUrls = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image" -> s"$Domain/image-api/raw/id",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio",
    ResourceType.File.toString -> Domain,
    ResourceType.H5P.toString -> H5PAddress
  )

  val ArticleApiHost: String = propOrElse("ARTICLE_API_HOST", "article-api.ndla-local")
  val ConceptApiHost: String = propOrElse("CONCEPT_API_HOST", "concept-api.ndla-local")
  val LearningpathApiHost: String = propOrElse("LEARNINGPATH_API_HOST", "learningpath-api.ndla-local")
  val AudioApiHost: String = propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")
  val DraftApiHost: String = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  val ImageApiHost: String = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  val SearchApiHost: String = propOrElse("SEARCH_API_HOST", "search-api.ndla-local")
  val ApiGatewayHost: String = propOrElse("API_GATEWAY_HOST", "api-gateway.ndla-local")

  val internalApiUrls: Map[String, String] = Map(
    "article-api" -> s"http://$ArticleApiHost/intern",
    "audio-api" -> s"http://$AudioApiHost/intern",
    "draft-api" -> s"http://$DraftApiHost/intern",
    "image-api" -> s"http://$ImageApiHost/intern"
  )

  lazy val BrightcoveAccountId: String = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  lazy val BrightcovePlayerId: String = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  lazy val BrightcoveVideoScriptUrl =
    s"//players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayerId}_default/index.min.js"
  val H5PResizerScriptUrl = "//h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js"
  val NRKVideoScriptUrl = Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")

  val SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-draft-api.ndla-local")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val DraftSearchIndex: String = propOrElse("SEARCH_INDEX_NAME", "draft-articles")
  val DraftTagSearchIndex: String = propOrElse("TAG_SEARCH_INDEX_NAME", "draft-tags")
  val DraftGrepCodesSearchIndex: String = propOrElse("GREP_CODES_SEARCH_INDEX_NAME", "draft-grepcodes")
  val AgreementSearchIndex: String = propOrElse("AGREEMENT_SEARCH_INDEX_NAME", "draft-agreements")
  val DraftSearchDocument = "article-drafts"
  val DraftTagSearchDocument = "article-drafts-tag"
  val DraftGrepCodesSearchDocument = "article-drafts-grepcodes"
  val AgreementSearchDocument = "agreement-drafts"
  val DefaultPageSize = 10
  val MaxPageSize = 10000
  val IndexBulkSize = 200
  val ElasticSearchIndexMaxResultWindow = 10000
  val ElasticSearchScrollKeepAlive = "1m"
  val InitialScrollContextKeywords = List("0", "initial", "start", "first")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  val AttachmentStorageName: String =
    propOrElse("ARTICLE_ATTACHMENT_S3_BUCKET", s"$Environment.article-attachments.ndla")

  lazy val H5PAddress: String = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "test" -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no",
      "ff" -> "https://h5p-ff.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  lazy val supportedUploadExtensions = Set(
    ".csv",
    ".doc",
    ".docx",
    ".dwg",
    ".dxf",
    ".ggb",
    ".ipynb",
    ".json",
    ".odp",
    ".ods",
    ".odt",
    ".pdf",
    ".pln",
    ".pro",
    ".ppt",
    ".pptx",
    ".pub",
    ".rtf",
    ".skp",
    ".stl",
    ".tex",
    ".tsv",
    ".txt",
    ".xls",
    ".xlsx",
    ".xml",
    ".f3d",
    ".mp4"
  )

  def booleanProp(key: String): Boolean = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOpt(key: String): Option[String] =
    propOrNone(key) match {
      case Some(prop) => Some(prop)
      case _          => None
    }

  def propOrElse(key: String, default: => String): String = propOpt(key).getOrElse(default)
}

/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.amazonaws.regions.Regions
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Environment.{booleanPropOrFalse, prop, propToAwsRegion}
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.common.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties._

trait Props extends HasBaseProps {
  val props: DraftApiProperties
}

class DraftApiProperties extends BaseProps with StrictLogging {
  def ApplicationName              = "draft-api"
  def Auth0LoginEndpoint           = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"
  def DraftRoleWithWriteAccess     = "drafts:write"
  def DraftRoleWithPublishAccess   = "drafts:publish"
  def ArticleRoleWithPublishAccess = "articles:publish"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String   = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int        = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String   = prop(PropertyKeys.MetaSchemaKey)
  def MetaMaxConnections   = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt

  def ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def externalApiUrls = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image"                 -> s"$Domain/image-api/raw/id",
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio",
    ResourceType.File.toString  -> Domain,
    ResourceType.H5P.toString   -> H5PAddress
  )

  def internalApiUrls: Map[String, String] = Map(
    "article-api" -> s"http://$ArticleApiHost/intern",
    "audio-api"   -> s"http://$AudioApiHost/intern",
    "draft-api"   -> s"http://$DraftApiHost/intern",
    "image-api"   -> s"http://$ImageApiHost/intern"
  )

  def AllowHtmlInTitle: Boolean = booleanPropOrFalse("ALLOW_HTML_IN_TITLE")

  def BrightcoveAccountId: String        = prop("NDLA_BRIGHTCOVE_ACCOUNT_ID")
  private def BrightcovePlayerId: String = prop("NDLA_BRIGHTCOVE_PLAYER_ID")

  def BrightcoveVideoScriptUrl =
    s"//players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayerId}_default/index.min.js"
  def H5PResizerScriptUrl = "//h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js"
  def NRKVideoScriptUrl = Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")

  def SearchServer: String              = propOrElse("SEARCH_SERVER", "http://search-draft-api.ndla-local")
  def DraftSearchIndex: String          = propOrElse("SEARCH_INDEX_NAME", "draft-articles")
  def DraftTagSearchIndex: String       = propOrElse("TAG_SEARCH_INDEX_NAME", "draft-tags")
  def DraftGrepCodesSearchIndex: String = propOrElse("GREP_CODES_SEARCH_INDEX_NAME", "draft-grepcodes")
  def AgreementSearchIndex: String      = propOrElse("AGREEMENT_SEARCH_INDEX_NAME", "draft-agreements")
  def DraftSearchDocument               = "article-drafts"
  def DraftTagSearchDocument            = "article-drafts-tag"
  def DraftGrepCodesSearchDocument      = "article-drafts-grepcodes"
  def AgreementSearchDocument           = "agreement-drafts"
  def DefaultPageSize                   = 10
  def MaxPageSize                       = 10000
  def IndexBulkSize                     = 200
  def ElasticSearchIndexMaxResultWindow = 10000
  def ElasticSearchScrollKeepAlive      = "1m"
  def InitialScrollContextKeywords      = List("0", "initial", "start", "first")

  def TaxonomyVersionHeader = "VersionHash"

  def AttachmentStorageName: String =
    propOrElse("ARTICLE_ATTACHMENT_S3_BUCKET", s"$Environment.article-attachments.ndla")

  def AttachmentStorageRegion: Regions = propToAwsRegion("ARTICLE_ATTACHMENT_S3_BUCKET_REGION")

  def H5PAddress: String = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "test"    -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no",
      "ff"      -> "https://h5p-ff.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  def supportedUploadExtensions = Set(
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
    ".mp4",
    ".ino",
    ".stp",
    ".step"
  )
}

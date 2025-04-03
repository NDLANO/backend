/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.database.{DatabaseProps, HasDatabaseProps}
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties.*

trait Props extends HasBaseProps with HasDatabaseProps {
  val props: DraftApiProperties
}

class DraftApiProperties extends BaseProps with DatabaseProps with StrictLogging {
  def ApplicationName            = "draft-api"
  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  private def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def externalApiUrls: Map[String, String] = Map(
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

  def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "sub", "sup")
  def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p", "strong")

  private def BrightcoveAccountId: String = prop("BRIGHTCOVE_ACCOUNT_ID")
  private def BrightcovePlayerId: String  = prop("BRIGHTCOVE_PLAYER_ID")

  def BrightcoveVideoScriptUrl: String =
    s"//players.brightcove.net/$BrightcoveAccountId/${BrightcovePlayerId}_default/index.min.js"
  def H5PResizerScriptUrl = "//h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js"
  def NRKVideoScriptUrl: Seq[String] =
    Seq("//www.nrk.no/serum/latest/js/video_embed.js", "//nrk.no/serum/latest/js/video_embed.js")

  def SearchServer: String                       = propOrElse("SEARCH_SERVER", "http://search-draft-api.ndla-local")
  def DraftSearchIndex: String                   = propOrElse("SEARCH_INDEX_NAME", "draft-articles")
  def DraftTagSearchIndex: String                = propOrElse("TAG_SEARCH_INDEX_NAME", "draft-tags")
  def DraftGrepCodesSearchIndex: String          = propOrElse("GREP_CODES_SEARCH_INDEX_NAME", "draft-grepcodes")
  def DraftSearchDocument                        = "article-drafts"
  def DraftTagSearchDocument                     = "article-drafts-tag"
  def DraftGrepCodesSearchDocument               = "article-drafts-grepcodes"
  def DefaultPageSize                            = 10
  def MaxPageSize                                = 10000
  def IndexBulkSize                              = 200
  def ElasticSearchIndexMaxResultWindow          = 10000
  def ElasticSearchScrollKeepAlive               = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def TaxonomyVersionHeader = "VersionHash"

  def AttachmentStorageName: String =
    propOrElse("ARTICLE_ATTACHMENT_S3_BUCKET", s"$Environment.article-attachments.ndla")

  def AttachmentStorageRegion: Option[String] = propOrNone("ARTICLE_ATTACHMENT_S3_BUCKET_REGION")

  def H5PAddress: String = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "local"   -> "https://h5p-test.ndla.no",
      "test"    -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  def supportedUploadExtensions: Set[String] = Set(
    ".3mf",
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

  def multipartFileSizeThresholdBytes: Int = 1024 * 1024 * 30 // 30MB
  val auth0ManagementClientId: String      = prop("AUTH0_MANAGEMENT_CLIENT_ID")
  val auth0ManagementClientSecret: String  = prop("AUTH0_MANAGEMENT_CLIENT_SECRET")

  override def MetaMigrationLocation: String      = "no/ndla/draftapi/db/migration"
  override def MetaMigrationTable: Option[String] = Some("schema_version")
}

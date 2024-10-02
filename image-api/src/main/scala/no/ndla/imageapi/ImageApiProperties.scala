/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Environment.prop
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.common.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}

import scala.util.Properties.*

trait Props extends HasBaseProps {
  val props: ImageApiProperties
}

class ImageApiProperties extends BaseProps with StrictLogging {
  def ApplicationName            = "image-api"
  val Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val RoleWithWriteAccess = "images:write"

  val ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  val HealthControllerPath          = "/health"
  val ImageApiBasePath              = "/image-api"
  val ApiDocsPath: String           = s"$ImageApiBasePath/api-docs"
  val ImageControllerV2Path: String = s"$ImageApiBasePath/v2/images"
  val ImageControllerV3Path: String = s"$ImageApiBasePath/v3/images"
  val RawControllerPath: String     = s"$ImageApiBasePath/raw"

  val ValidFileExtensions: Seq[String] = Seq(".jpg", ".png", ".jpeg", ".bmp", ".gif", ".svg", ".jfif")

  val ValidMimeTypes: Seq[String] = Seq(
    "image/bmp",
    "image/gif",
    "image/jpeg",
    "image/x-citrix-jpeg",
    "image/pjpeg",
    "image/png",
    "image/x-citrix-png",
    "image/x-png",
    "image/svg+xml"
  )

  val oldCreatorTypes: List[String] = List(
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

  val creatorTypes: List[String] = List(
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

  val oldProcessorTypes: List[String] =
    List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  val processorTypes: List[String] =
    List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  val oldRightsholderTypes: List[String] = List("rettighetshaver", "forlag", "distributør", "leverandør")
  val rightsholderTypes: List[String]    = List("rightsholder", "publisher", "distributor", "supplier")

  val allowedAuthors: Seq[String] = creatorTypes ++ processorTypes ++ rightsholderTypes

  val IsoMappingCacheAgeInMs: Int     = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs: Int = 1000 * 60 * 60 // 1 hour caching

  val MaxImageFileSizeBytes: Int    = 1024 * 1024 * 40 // 40 MiB
  val ImageScalingUltraMinSize: Int = 640
  val ImageScalingUltraMaxSize: Int = 2080

  def MetaMaxConnections: Int = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt
  def MetaUserName: String    = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String    = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String    = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String      = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int           = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String      = prop(PropertyKeys.MetaSchemaKey)

  val StorageName: String           = propOrElse("IMAGE_FILE_S3_BUCKET", s"$Environment.images.ndla")
  val StorageRegion: Option[String] = propOrNone("IMAGE_FILE_S3_BUCKET_REGION")

  val S3NewFileCacheControlHeader: String = propOrElse("IMAGE_FILE_S3_BUCKET_CACHE_CONTROL", "max-age=2592000")

  val SearchIndex: String    = propOrElse("SEARCH_INDEX_NAME", "images")
  val SearchDocument         = "image"
  val TagSearchIndex: String = propOrElse("TAG_SEARCH_INDEX_NAME", "tags")
  val TagSearchDocument      = "tag"

  val DefaultPageSize: Int                       = 10
  val MaxPageSize: Int                           = 10000
  val IndexBulkSize                              = 1000
  val SearchServer: String                       = propOrElse("SEARCH_SERVER", "http://search-image-api.ndla-local")
  val ElasticSearchIndexMaxResultWindow          = 10000
  val ElasticSearchScrollKeepAlive               = "1m"
  val InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  lazy val Domain: String       = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))
  val ImageApiV2UrlBase: String = Domain + ImageControllerV2Path + "/"
  val ImageApiV3UrlBase: String = Domain + ImageControllerV3Path + "/"
  val RawImageUrlBase: String   = Domain + RawControllerPath
}

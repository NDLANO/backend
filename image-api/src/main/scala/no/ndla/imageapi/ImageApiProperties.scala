/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.BaseProps
import no.ndla.database.DatabaseProps
import no.ndla.network.{AuthUser, Domains}

import scala.util.Properties.*

type Props = ImageApiProperties

class ImageApiProperties extends BaseProps with DatabaseProps with StrictLogging {
  def ApplicationName            = "image-api"
  val Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

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
    "image/svg+xml",
  )

  val IsoMappingCacheAgeInMs: Int     = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs: Int = 1000 * 60 * 60 // 1 hour caching

  val MaxImageFileSizeBytes: Int    = 1024 * 1024 * 40 // 40 MiB
  val ImageScalingUltraMinSize: Int = propOrElse("IMAGE_SCALING_ULTRA_MIN_SIZE", "640").toInt
  val ImageScalingUltraMaxSize: Int = propOrElse("IMAGE_SCALING_ULTRA_MAX_SIZE", "2080").toInt

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
  val RawImageUrlBase: String   = propOrElse("IMAGE_API_CLOUDFRONT_DOMAIN", Domain + RawControllerPath)

  override def MetaMigrationLocation: String      = "no/ndla/imageapi/db/migration"
  override def MetaMigrationTable: Option[String] = Some("schema_version")
}

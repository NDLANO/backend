/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.database.{DatabaseProps, HasDatabaseProps}
import no.ndla.network.{AuthUser, Domains}

import scala.util.Properties.*

trait Props extends HasBaseProps with HasDatabaseProps {
  val props: AudioApiProperties
}

class AudioApiProperties extends BaseProps with DatabaseProps with StrictLogging {
  val IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  def ApplicationName            = "audio-api"
  val Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  val AudioControllerPath  = "/audio-api/v1/audio/"
  val SeriesControllerPath = "/audio-api/v1/series/"

  val MaxAudioFileSizeBytes: Int = 1024 * 1024 * 100 // 100 MiB

  val StorageName: String           = propOrElse("AUDIO_FILE_S3_BUCKET", s"$Environment.audio.ndla")
  val StorageRegion: Option[String] = propOrNone("AUDIO_FILE_S3_BUCKET_REGION")

  val SearchServer: String                 = propOrElse("SEARCH_SERVER", "http://search-audio-api.ndla-local")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndex: String                  = propOrElse("SEARCH_INDEX_NAME", "audios")
  val SearchDocument                       = "audio"
  val SeriesSearchIndex: String            = propOrElse("SERIES_SEARCH_INDEX_NAME", "series")
  val SeriesSearchDocument                 = "series"
  val AudioTagSearchIndex: String          = propOrElse("AUDIO_TAG_SEARCH_INDEX_NAME", "tags-audios")
  val AudioTagSearchDocument               = "audio-tag"
  val DefaultPageSize                      = 10
  val MaxPageSize                          = 10000
  val IndexBulkSize                        = 200

  val IsoMappingCacheAgeInMs: Int                = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs: Int            = 1000 * 60 * 60 // 1 hour caching
  val ElasticSearchIndexMaxResultWindow          = 10000
  val ElasticSearchScrollKeepAlive               = "1m"
  val InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  val AudioFilesUrlSuffix = "audio/files"

  val creatorTypeMap: Map[String, String] = Map(
    "opphavsmann"    -> "originator",
    "fotograf"       -> "photographer",
    "kunstner"       -> "artist",
    "forfatter"      -> "writer",
    "manusforfatter" -> "scriptwriter",
    "innleser"       -> "reader",
    "oversetter"     -> "translator",
    "regissør"       -> "director",
    "illustratør"    -> "illustrator",
    "medforfatter"   -> "cowriter",
    "komponist"      -> "composer"
  )

  val processorTypeMap: Map[String, String] = Map(
    "bearbeider"     -> "processor",
    "tilrettelegger" -> "facilitator",
    "redaksjonelt"   -> "editorial",
    "språklig"       -> "linguistic",
    "ide"            -> "idea",
    "sammenstiller"  -> "compiler",
    "korrektur"      -> "correction"
  )

  val rightsholderTypeMap: Map[String, String] = Map(
    "rettighetshaver" -> "rightsholder",
    "forlag"          -> "publisher",
    "distributør"     -> "distributor",
    "leverandør"      -> "supplier"
  )

  lazy val Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  lazy val RawImageApiUrl: String = s"$Domain/image-api/raw/id"

}

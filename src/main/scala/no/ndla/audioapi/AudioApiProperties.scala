/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{AuthUser, Domains}
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets._

import scala.util.Properties._
import scala.util.{Failure, Success}

object AudioApiProperties extends LazyLogging {
  val IsKubernetes: Boolean = envOrNone("NDLA_IS_KUBERNETES").isDefined

  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "audio-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val RoleWithWriteAccess = "audio:write"

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "support+api@ndla.no"
  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
  val AudioControllerPath = "/audio-api/v1/audio/"

  val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource = prop(PropertyKeys.MetaResourceKey)
  val MetaServer = prop(PropertyKeys.MetaServerKey)
  val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = prop(PropertyKeys.MetaSchemaKey)
  val MetaInitialConnections = 3
  val MetaMaxConnections = 10

  val MaxAudioFileSizeBytes = 1024 * 1024 * 100 // 100 MiB

  val StorageName: String = propOrElse("AUDIO_FILE_S3_BUCKET", s"$Environment.audio.ndla")

  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-audio-api.ndla-local")
  val DraftApiHost = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "audios")
  val SearchDocument = "audio"
  val DefaultPageSize = 10
  val MaxPageSize = 10000
  val IndexBulkSize = 200

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val NdlaRedUsername = prop("NDLA_RED_USERNAME")
  val NdlaRedPassword = prop("NDLA_RED_PASSWORD")

  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val ElasticSearchIndexMaxResultWindow = 10000
  val ElasticSearchScrollKeepAlive = "1m"
  val InitialScrollContextKeywords = List("0", "initial", "start", "first")

  val AudioFilesUrlSuffix = "audio/files"

  val creatorTypeMap = Map(
    "opphavsmann" -> "originator",
    "fotograf" -> "photographer",
    "kunstner" -> "artist",
    "forfatter" -> "writer",
    "manusforfatter" -> "scriptwriter",
    "innleser" -> "reader",
    "oversetter" -> "translator",
    "regissør" -> "director",
    "illustratør" -> "illustrator",
    "medforfatter" -> "cowriter",
    "komponist" -> "composer"
  )

  val processorTypeMap = Map(
    "bearbeider" -> "processor",
    "tilrettelegger" -> "facilitator",
    "redaksjonelt" -> "editorial",
    "språklig" -> "linguistic",
    "ide" -> "idea",
    "sammenstiller" -> "compiler",
    "korrektur" -> "correction"
  )

  val rightsholderTypeMap = Map(
    "rettighetshaver" -> "rightsholder",
    "forlag" -> "publisher",
    "distributør" -> "distributor",
    "leverandør" -> "supplier"
  )

  lazy val Domain = Domains.get(Environment)

  lazy val secrets = {
    val SecretsFile = "audio-api.secrets"
    readSecrets(SecretsFile) match {
      case Success(values) => values
      case Failure(exception) =>
        throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
    }
  }

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    propOrNone(key) match {
      case Some(prop)            => prop
      case None if !IsKubernetes => secrets.get(key).flatten.getOrElse(default)
      case _                     => default
    }
  }

}

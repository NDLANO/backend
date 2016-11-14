/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets._

import scala.util.Properties._

object AudioApiProperties extends LazyLogging {
  val SecretsFile = "audio_api.secrets"
  lazy val secrets = readSecrets(SecretsFile).getOrElse(throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile"))
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")

  val ApplicationPort = 80
  val ContactEmail = "christergundersen@ndla.no"
  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource = prop(PropertyKeys.MetaResourceKey)
  val MetaServer = prop(PropertyKeys.MetaServerKey)
  val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = prop(PropertyKeys.MetaSchemaKey)
  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val StorageName = s"$Environment.audio.ndla"

  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-audio-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndex = "audios"
  val SearchDocument = "audio"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 1000

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val MappingHost = "mapping-api.ndla-local"
  val IsoMappingCacheAgeInMs = 1000 * 60 * 60
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  val AudioFilesUrlSuffix = "audio/files"
  val Domain = Map(
    "local" -> "http://localhost",
    "prod" -> "http://api.ndla.no"
  ).getOrElse(Environment, s"http://api.$Environment.ndla.no")


  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    secrets.get(key).flatten match {
      case Some(secret) => secret
      case None =>
        envOrNone(key) match {
          case Some(env) => env
          case None => default
        }
    }
  }
}
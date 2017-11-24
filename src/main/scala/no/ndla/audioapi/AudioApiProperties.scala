/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.Domains
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets._

import scala.util.Properties._
import scala.util.{Failure, Success}

object AudioApiProperties extends LazyLogging {
  val RoleWithWriteAccess = "audio:write"

  val SecretsFile = "audio-api.secrets"
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "christergundersen@ndla.no"
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
  val MetaMaxConnections = 20

  val MaxAudioFileSizeBytes = 1024 * 1024 * 40 // 40 MiB

  val StorageName = s"$Environment.audio.ndla"

  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-audio-api.ndla-local")
  val DraftApiHost = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "audios")
  val SearchDocument = "audio"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 200

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val ElasticSearchIndexMaxResultWindow = 10000

  val AudioFilesUrlSuffix = "audio/files"


  val oldCreatorTypes = List("opphavsmann", "fotograf", "kunstner", "redaksjonelt", "forfatter", "manusforfatter", "innleser", "oversetter", "regissør", "illustratør", "medforfatter", "komponist")
  val creatorTypes = List("originator", "photographer", "artist", "editorial", "writer", "scriptwriter", "reader", "translator", "director", "illustrator", "cowriter", "composer")

  val oldProcessorTypes = List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  val processorTypes = List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  val oldRightsholderTypes = List("rettighetshaver", "forlag", "distributør", "leverandør")
  val rightsholderTypes = List("rightsholder", "publisher", "distributor", "supplier")
  val allowedAuthors = AudioApiProperties.creatorTypes ++ AudioApiProperties.processorTypes ++ AudioApiProperties.rightsholderTypes


  lazy val Domain = Domains.get(Environment)

  lazy val secrets = readSecrets(SecretsFile) match {
     case Success(values) => values
     case Failure(exception) => throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
   }

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
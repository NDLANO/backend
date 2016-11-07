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

import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Properties, Success, Try}

object AudioApiProperties extends LazyLogging {
  var AudioApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  lazy val ApplicationPort = 80
  lazy val ContactEmail = "christergundersen@ndla.no"

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  lazy val MetaUserName = get(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = get(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = get(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = get(PropertyKeys.MetaServerKey)
  lazy val MetaPort = getInt(PropertyKeys.MetaPortKey)
  lazy val MetaSchema = get(PropertyKeys.MetaSchemaKey)
  lazy val MetaInitialConnections = 3
  lazy val MetaMaxConnections = 20

  lazy val StorageName = get("NDLA_ENVIRONMENT") + ".audio.ndla"

  lazy val SearchServer = getOrElse("SEARCH_SERVER", "http://search-audio-api.ndla-local")
  lazy val SearchRegion = getOrElse("SEARCH_REGION", "eu-central-1")
  lazy val SearchIndex = "audios"
  lazy val SearchDocument = "audio"
  lazy val DefaultPageSize: Int = 10
  lazy val MaxPageSize: Int = 100
  lazy val IndexBulkSize = 1000
  lazy val RunWithSignedSearchRequests: Boolean = getOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  lazy val MappingHost = "mapping-api.ndla-local"
  val IsoMappingCacheAgeInMs = 1000 * 60 * 60
  // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  lazy val Environment = get("NDLA_ENVIRONMENT")
  lazy val Domain = getDomain
  val AudioFilesUrlSuffix = "audio/files"

  def setProperties(properties: Map[String, Option[String]]) = {
    val missingProperties = AudioApiProps.filter(entry => entry._2.isEmpty).toList
    missingProperties.isEmpty match {
      case true => Success(properties.foreach(prop => AudioApiProps.put(prop._1, prop._2)))
      case false => Failure(new Exception(s"Missing the following properties: ${missingProperties.mkString(", ")}"))
    }
  }

  private def getOrElse(envKey: String, defaultValue: String) = {
    AudioApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => defaultValue
    }
  }

  private def getDomain: String = {
    Map("local" -> "http://localhost",
      "prod" -> "http://api.ndla.no"
    ).getOrElse(Environment, s"http://api.$Environment.ndla.no")
  }

  private def get(envKey: String): String = {
    AudioApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  private def getInt(envKey: String): Integer = {
    get(envKey).toInt
  }

  private def getBoolean(envKey: String): Boolean = {
    get(envKey).toBoolean
  }
}

object PropertiesLoader extends LazyLogging {
  val EnvironmentFile = "/audio-api.env"

  def readPropertyFile() = {
    Try(Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().map(key => key -> Properties.envOrNone(key)).toMap)
  }

  def load() = {
    val verification = for {
      file <- readPropertyFile()
      secrets <- readSecrets("audio_api.secrets")
      didSetProperties <- AudioApiProperties.setProperties(file ++ secrets)
    } yield didSetProperties

    if (verification.isFailure) {
      logger.error("Unable to load properties", verification.failed.get)
      System.exit(1)
    }
  }
}

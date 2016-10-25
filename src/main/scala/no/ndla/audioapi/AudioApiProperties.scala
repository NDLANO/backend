/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.io.Source

object AudioApiProperties extends LazyLogging {
  var AudioApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  lazy val ApplicationPort = getInt("APPLICATION_PORT")
  lazy val ContactEmail = get("CONTACT_EMAIL")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  lazy val MetaUserName = get("META_USER_NAME")
  lazy val MetaPassword = get("META_PASSWORD")
  lazy val MetaResource = get("META_RESOURCE")
  lazy val MetaServer = get("META_SERVER")
  lazy val MetaPort = getInt("META_PORT")
  lazy val MetaInitialConnections = getInt("META_INITIAL_CONNECTIONS")
  lazy val MetaMaxConnections = getInt("META_MAX_CONNECTIONS")
  lazy val MetaSchema = get("META_SCHEMA")

  lazy val SearchIndex = get("SEARCH_INDEX")
  lazy val SearchDocument = get("SEARCH_DOCUMENT")
  lazy val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  lazy val RunWithSignedSearchRequests: Boolean = getBoolean("RUN_WITH_SIGNED_SEARCH_REQUESTS")
  lazy val SearchServer = get("SEARCH_SERVER")
  lazy val SearchRegion = get("SEARCH_REGION")
  lazy val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  lazy val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  lazy val StorageName = get("STORAGE_NAME")

  lazy val MappingHost = get("MAPPING_API_HOST")
  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  lazy val Domain = get("DOMAIN")
  val AudioFilesUrlSuffix = "audio/files"

  def verify() = {
    val missingProperties = AudioApiProps.filter(entry => entry._2.isEmpty).toList
    if(missingProperties.nonEmpty){
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
    }
  }

  def setProperties(properties: Map[String, Option[String]]) = {
    properties.foreach(prop => AudioApiProps.put(prop._1, prop._2))
  }

  private def get(envKey: String): String = {
    AudioApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  private def getInt(envKey: String):Integer = {
    get(envKey).toInt
  }

  private def getBoolean(envKey: String): Boolean = {
    get(envKey).toBoolean
  }
}

object PropertiesLoader {
  val EnvironmentFile = "/audio-api.env"

  def readPropertyFile(): Map[String, Option[String]] = {
    val keys = Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$"))
    keys.map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }

  def load() = {
    AudioApiProperties.setProperties(readPropertyFile())
    AudioApiProperties.verify()
  }
}

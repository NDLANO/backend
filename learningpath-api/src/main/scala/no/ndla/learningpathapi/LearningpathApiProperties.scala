/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.database.{DatabaseProps, HasDatabaseProps}
import no.ndla.network.{AuthUser, Domains}

import scala.util.Properties.*

trait Props extends HasBaseProps with HasDatabaseProps {
  val props: LearningpathApiProperties
}

class LearningpathApiProperties extends BaseProps with DatabaseProps with StrictLogging {
  def ApplicationName            = "learningpath-api"
  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def SearchIndex: String = propOrElse("SEARCH_INDEX_NAME", "learningpaths")
  def SearchDocument      = "learningpath"
  def DefaultPageSize     = 10
  def MaxPageSize         = 10000
  def IndexBulkSize       = 1000

  object ExternalApiUrls {
    def ImageApiUrl    = s"$Domain/image-api/v3/images"
    def ImageApiRawUrl = s"$Domain/image-api/raw"
  }

  def NdlaFrontendHost: String = propOrElse(
    "NDLA_FRONTEND_HOST",
    Environment match {
      case "prod"  => "ndla.no"
      case "local" => "localhost:30017"
      case _       => s"$Environment.ndla.no"
    }
  )

  def NdlaFrontendProtocol: String = propOrElse(
    "NDLA_FRONTEND_PROTOCOL",
    Environment match {
      case "local" => "http"
      case _       => "https"
    }
  )

  def EnvironmentUrls(env: String): Set[String] = {
    Set(
      s"$env.ndla.no",
      s"www.$env.ndla.no",
      s"ndla-frontend.$env.api.ndla.no"
    )
  }

  def NdlaFrontendHostNames: Set[String] = Set(
    "ndla.no",
    "www.ndla.no",
    s"ndla-frontend.api.ndla.no",
    "localhost"
  ) ++
    EnvironmentUrls(Environment) ++
    EnvironmentUrls("test") ++
    EnvironmentUrls("staging")

  def ElasticSearchIndexMaxResultWindow          = 10000
  def ElasticSearchScrollKeepAlive               = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def AllowedHtmlTags: List[String] = List(
    "a",
    "b",
    "blockquote",
    "br",
    "cite",
    "code",
    "dd",
    "dl",
    "dt",
    "em",
    "i",
    "li",
    "ol",
    "p",
    "pre",
    "q",
    "section",
    "small",
    "strike",
    "strong",
    "sub",
    "sup",
    "u",
    "ul"
  )

  def SearchServer: String =
    propOrElse("SEARCH_SERVER", "http://search-learningpath-api.ndla-local")

  override def MetaMigrationLocation: String      = "no/ndla/learningpathapi/db/migration"
  override def MetaMigrationTable: Option[String] = Some("schema_version")
}

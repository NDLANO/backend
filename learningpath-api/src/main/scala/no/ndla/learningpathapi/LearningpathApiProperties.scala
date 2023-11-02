/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Environment.prop
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.common.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}

import scala.util.Properties._

trait Props extends HasBaseProps {
  val props: LearningpathApiProperties
}

class LearningpathApiProperties extends BaseProps with StrictLogging {
  def IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  def ApplicationName    = "learningpath-api"
  def Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  def MaxFolderDepth: Long    = propOrElse("MAX_FOLDER_DEPTH", "5").toLong

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def SearchIndex: String = propOrElse("SEARCH_INDEX_NAME", "learningpaths")
  def SearchDocument      = "learningpath"
  def DefaultPageSize     = 10
  def MaxPageSize         = 10000
  def IndexBulkSize       = 1000

  def InternalImageApiUrl = s"$ImageApiHost/image-api/v2/images"

  def RedisHost: String = propOrElse("REDIS_HOST", "redis")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

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

  def UsernameHeader = "X-Consumer-Username"

  def ElasticSearchIndexMaxResultWindow          = 10000
  def ElasticSearchScrollKeepAlive               = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def BasicHtmlTags: List[String] = List(
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
    "small",
    "strike",
    "strong",
    "sub",
    "sup",
    "u",
    "ul"
  )

  def MetaUserName: String    = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String    = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String    = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String      = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int           = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String      = prop(PropertyKeys.MetaSchemaKey)
  def MetaMaxConnections: Int = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt

  def SearchServer: String =
    propOrElse("SEARCH_SERVER", "http://search-learningpath-api.ndla-local")

  def RunWithSignedSearchRequests: Boolean =
    propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
}

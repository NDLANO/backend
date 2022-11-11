/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.Environment.prop
import no.ndla.common.configuration.BaseProps
import no.ndla.common.secrets.PropertyKeys
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties._

trait Props {
  val props: ConceptApiProperties
}

class ConceptApiProperties extends BaseProps with LazyLogging {
  def IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  def ApplicationName = "concept-api"

  def Auth0LoginEndpoint =
    s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"
  def ConceptRoleWithWriteAccess = "concept:write"

  def ApplicationPort         = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  def ContactName: String     = propOrElse("CONTACT_NAME", "NDLA")
  def ContactUrl: String      = propOrElse("CONTACT_URL", "https://ndla.no")
  def ContactEmail: String    = propOrElse("CONTACT_EMAIL", "hjelp+api@ndla.no")
  def TermsUrl: String        = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  def MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String   = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int        = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String   = prop(PropertyKeys.MetaSchemaKey)
  def MetaMaxConnections   = 10

  def resourceHtmlEmbedTag         = "embed"
  def ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching

  def ArticleApiHost: String = propOrElse("ARTICLE_API_HOST", "article-api.ndla-local")
  def ImageApiHost: String   = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  def ApiGatewayHost: String = propOrElse("API_GATEWAY_HOST", "api-gateway.ndla-local")

  def SearchApiHost: String                = propOrElse("SEARCH_API_HOST", "search-api.ndla-local")
  def SearchServer: String                 = propOrElse("SEARCH_SERVER", "http://search-concept-api.ndla-local")
  def RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  def DraftConceptSearchIndex: String     = propOrElse("CONCEPT_SEARCH_INDEX_NAME", "concepts")
  def PublishedConceptSearchIndex: String = propOrElse("PUBLISHED_CONCEPT_SEARCH_INDEX_NAME", "publishedconcepts")
  def ConceptSearchDocument               = "concept"
  def DefaultPageSize                     = 10
  def MaxPageSize                         = 10000
  def IndexBulkSize                       = 250
  def ElasticSearchIndexMaxResultWindow   = 10000
  def ElasticSearchScrollKeepAlive        = "1m"
  def InitialScrollContextKeywords        = List("0", "initial", "start", "first")

  def CorrelationIdKey    = "correlationID"
  def CorrelationIdHeader = "X-Correlation-ID"

  def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  def H5PAddress = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "test"    -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  def externalApiUrls: Map[String, String] = Map(
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image"                 -> s"$Domain/image-api/raw/id",
    ResourceType.H5P.toString   -> H5PAddress
  )
}

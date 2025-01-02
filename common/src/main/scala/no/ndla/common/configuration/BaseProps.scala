package no.ndla.common.configuration

import sttp.client3.UriContext
import sttp.model.Uri

import scala.util.Properties.{propOrElse, propOrNone}

trait BaseProps {
  def intPropOrDefault(name: String, default: Int): Int = propOrNone(name).flatMap(_.toIntOption).getOrElse(default)
  def booleanPropOrNone(name: String): Option[Boolean]  = propOrNone(name).flatMap(_.toBooleanOption)
  def booleanPropOrElse(name: String, default: => Boolean): Boolean = booleanPropOrNone(name).getOrElse(default)

  def ApplicationPort: Int
  def ApplicationName: String

  private def setLogProperties(): Unit = {
    System.setProperty("APPLICATION_NAME", ApplicationName): Unit
  }

  setLogProperties()

  def Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")

  def ContactName: String  = propOrElse("CONTACT_NAME", "NDLA")
  def ContactUrl: String   = propOrElse("CONTACT_URL", "https://ndla.no")
  def ContactEmail: String = propOrElse("CONTACT_EMAIL", "hjelp+api@ndla.no")
  def TermsUrl: String     = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  def ApiGatewayHost: String      = propOrElse("API_GATEWAY_HOST", "api-gateway.ndla-local")
  def ArticleApiHost: String      = propOrElse("ARTICLE_API_HOST", "article-api.ndla-local")
  def AudioApiHost: String        = propOrElse("AUDIO_API_HOST", "audio-api.ndla-local")
  def ConceptApiHost: String      = propOrElse("CONCEPT_API_HOST", "concept-api.ndla-local")
  def DraftApiHost: String        = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  def ImageApiHost: String        = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  def MyNDLAApiHost: String       = propOrElse("MYNDLA_API_HOST", "myndla-api.ndla-local")
  def LearningpathApiHost: String = propOrElse("LEARNINGPATH_API_HOST", "learningpath-api.ndla-local")
  def SearchApiHost: String       = propOrElse("SEARCH_API_HOST", "search-api.ndla-local")
  def FrontpageApiHost: String    = propOrElse("FRONTPAGE_API_HOST", "frontpage-api.ndla-local")
  def TaxonomyApiHost: String     = propOrElse("TAXONOMY_API_HOST", "taxonomy-api.ndla-local:5000")

  def ApiGatewayUrl: String      = s"http://$ApiGatewayHost"
  def ArticleApiUrl: String      = s"http://$ArticleApiHost"
  def AudioApiUrl: String        = s"http://$AudioApiHost"
  def ConceptApiUrl: String      = s"http://$ConceptApiHost"
  def DraftApiUrl: String        = s"http://$DraftApiHost"
  def GrepApiUrl: String         = s"https://${propOrElse("GREP_API_HOST", "data.udir.no")}"
  def ImageApiUrl: String        = s"http://$ImageApiHost"
  def LearningpathApiUrl: String = s"http://$LearningpathApiHost"
  def SearchApiUrl: String       = s"http://$SearchApiHost"
  def FrontpageApiUrl: String    = s"http://$FrontpageApiHost"
  def TaxonomyUrl: String        = s"http://$TaxonomyApiHost"
  def disableWarmup: Boolean     = booleanPropOrElse("DISABLE_WARMUP", default = false)

  def MAX_SEARCH_THREADS: Int    = intPropOrDefault("MAX_SEARCH_THREADS", 100)
  def SEARCH_INDEX_SHARDS: Int   = intPropOrDefault("SEARCH_INDEX_SHARDS", 1)
  def SEARCH_INDEX_REPLICAS: Int = intPropOrDefault("SEARCH_INDEX_REPLICAS", 1)

  def TAPIR_THREADS: Int = intPropOrDefault("TAPIR_THREADS", 100)

  def brightCoveAuthUri: String = s"https://oauth.brightcove.com/v4/access_token"
  def brightCoveVideoUri(accountId: String, videoId: String): Uri =
    uri"https://cms.api.brightcove.com/v1/accounts/$accountId/videos/$videoId/sources"

}

/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.configuration

import com.typesafe.scalalogging.StrictLogging
import sttp.client3.UriContext
import sttp.model.Uri

import scala.collection.mutable
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Properties.{propOrElse, propOrNone}
import scala.util.{Failure, Success, Try}

trait BaseProps extends StrictLogging {

  /** Mutable variable to store state of properties, enables loading properties and crashing _optionally_ at startup.
    * Since applications can start with configurations that doesn't require all required properties (ex: generating
    * openapi documentation)
    */
  private val loadedProps: mutable.Map[String, Prop[?]] = mutable.Map.empty

  def throwIfFailedProps(): Unit = {
    val failedProps: List[FailedProp[?]] = {
      loadedProps.values.toList.collect { case Prop(FailedProp(key, ex)) => FailedProp(key, ex) }
    }

    if (failedProps.nonEmpty) {
      val failedKeys = failedProps
        .map(failed => failed.key)
        .mkString("[", ", ", "]")

      val mainException = EnvironmentNotFoundException(s"Unable to load the following properties: $failedKeys")
      failedProps.map { case FailedProp(_, ex) => mainException.addSuppressed(ex) }: Unit
      throw mainException
    }
  }

  def intPropOrDefault(name: String, default: Int): Int = propOrNone(name).flatMap(_.toIntOption).getOrElse(default)
  def booleanPropOrNone(name: String): Option[Boolean]  = propOrNone(name).flatMap(_.toBooleanOption)
  def booleanPropOrElse(name: String, default: => Boolean): Boolean = booleanPropOrNone(name).getOrElse(default)

  /** Test method to update props for tests */
  def propFromTestValue[T](key: String, value: T): Prop[T] = {
    val prop = Prop.successful[T](key, value)
    loadedProps.get(key) match {
      case Some(existing: Prop[T] @unchecked) => existing.setValue(value)
      case Some(_)                            => throw new RuntimeException(s"Bad type when updating prop $key")
      case None                               => loadedProps.put(key, prop): Unit
    }
    prop
  }

  def prop(key: String): Prop[String] = {
    val propToAdd = propOrNone(key) match {
      case Some(value) =>
        Prop.successful(key, value)
      case None =>
        Prop.failed[String](key)
    }
    loadedProps.put(key, propToAdd): Unit
    propToAdd
  }

  def propMap[T, R](prop: Prop[T])(f: T => R): Prop[R] = {
    val newProp = prop.reference match {
      case LoadedProp(k, v) =>
        Try(f(v)) match {
          case Failure(exception) =>
            val nfe = EnvironmentNotFoundException.singleKey(k)
            nfe.initCause(exception)
            Prop(FailedProp[R](k, nfe))
          case Success(result) => Prop(LoadedProp[R](k, result))
        }
      case FailedProp(k, e) => Prop(FailedProp[R](k, e))
    }
    loadedProps.put(prop.key, newProp): Unit
    newProp
  }

  def booleanPropOrFalse(key: String): Boolean = {
    propOrNone(key).flatMap(_.toBooleanOption).getOrElse(false)
  }

  def ApplicationPort: Int
  def ApplicationName: String

  private def setLogProperties(): Unit = System.setProperty("APPLICATION_NAME", ApplicationName): Unit
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

  def SupportedLanguages: List[String] =
    propOrElse("SUPPORTED_LANGUAGES", "nb,nn,en,sma,se,de,es,zh,ukr").split(",").toList

  def ndlaFrontendUrl: String = Environment match {
    case "local" => "http://localhost:30017"
    case "prod"  => "https://ndla.no"
    case _       => s"https://$Environment.ndla.no"
  }

  def MAX_SEARCH_THREADS: Int    = intPropOrDefault("MAX_SEARCH_THREADS", 100)
  def SEARCH_INDEX_SHARDS: Int   = intPropOrDefault("SEARCH_INDEX_SHARDS", 1)
  def SEARCH_INDEX_REPLICAS: Int = intPropOrDefault("SEARCH_INDEX_REPLICAS", 1)

  def TAPIR_THREADS: Int = intPropOrDefault("TAPIR_THREADS", 100)

  def BrightCoveAuthUri: String                                   = s"https://oauth.brightcove.com/v4/access_token"
  def BrightCoveVideoUri(accountId: String, videoId: String): Uri =
    uri"https://cms.api.brightcove.com/v1/accounts/$accountId/videos/$videoId/sources"

  def DisableLicense: Boolean = booleanPropOrElse("DISABLE_LICENSE", default = false)

  def ReadinessProbeDetectionTimeoutSeconds: Duration = intPropOrDefault("READINESS_PROBE_DETECTION_SECONDS", 7).seconds

}

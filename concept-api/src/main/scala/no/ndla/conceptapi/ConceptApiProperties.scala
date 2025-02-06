/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.database.{DatabaseProps, HasDatabaseProps}
import no.ndla.network.{AuthUser, Domains}
import no.ndla.validation.ResourceType

import scala.util.Properties.*

trait Props extends HasBaseProps with HasDatabaseProps {
  val props: ConceptApiProperties
}

class ConceptApiProperties extends BaseProps with DatabaseProps with StrictLogging {
  def IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  def ApplicationName = "concept-api"

  def Auth0LoginEndpoint: String =
    s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  def DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")

  def SearchServer: String                = propOrElse("SEARCH_SERVER", "http://search-concept-api.ndla-local")
  def DraftConceptSearchIndex: String     = propOrElse("CONCEPT_SEARCH_INDEX_NAME", "concepts")
  def PublishedConceptSearchIndex: String = propOrElse("PUBLISHED_CONCEPT_SEARCH_INDEX_NAME", "publishedconcepts")
  def ConceptSearchDocument               = "concept"
  def DefaultPageSize                     = 10
  def MaxPageSize                         = 10000
  def IndexBulkSize                       = 250
  def ElasticSearchIndexMaxResultWindow   = 10000
  def ElasticSearchScrollKeepAlive        = "1m"
  def InitialScrollContextKeywords: List[String] = List("0", "initial", "start", "first")

  def IntroductionHtmlTags: Set[String] = Set("br", "code", "em", "p", "span", "strong", "sub", "sup")

  private def Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  private def H5PAddress: String = propOrElse(
    "NDLA_H5P_ADDRESS",
    Map(
      "local"   -> "https://h5p-test.ndla.no",
      "test"    -> "https://h5p-test.ndla.no",
      "staging" -> "https://h5p-staging.ndla.no"
    ).getOrElse(Environment, "https://h5p.ndla.no")
  )

  def externalApiUrls: Map[String, String] = Map(
    ResourceType.Audio.toString -> s"$Domain/audio-api/v1/audio",
    ResourceType.H5P.toString   -> H5PAddress,
    ResourceType.Image.toString -> s"$Domain/image-api/v2/images",
    "raw-image"                 -> s"$Domain/image-api/raw/id"
  )

  override def MetaMigrationLocation: String = "no/ndla/conceptapi/db/migration"
}

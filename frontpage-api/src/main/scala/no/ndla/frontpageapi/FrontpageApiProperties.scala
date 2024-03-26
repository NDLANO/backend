/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import no.ndla.common.Environment.prop
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.network.{AuthUser, Domains}
import no.ndla.common.secrets.PropertyKeys

import scala.util.Properties._

trait Props extends HasBaseProps {
  val props: FrontpageApiProperties
}

class FrontpageApiProperties extends BaseProps {
  val IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  val ApplicationName            = "frontpage-api"
  val ApplicationPort: Int       = propOrElse("APPLICATION_PORT", "80").toInt
  val NumThreads: Int            = propOrElse("NUM_THREADS", "200").toInt
  val DefaultLanguage: String    = propOrElse("DEFAULT_LANGUAGE", "nb")
  val Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val SecretsFile = "frontpage-api.secrets"

  def MetaUserName: String    = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String    = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String    = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String      = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int           = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String      = prop(PropertyKeys.MetaSchemaKey)
  def MetaMaxConnections: Int = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt
  val DefaultPageSize         = 10

  val Domain: String         = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))
  val RawImageApiUrl: String = s"$Domain/image-api/raw"

  val BrightcoveAccountId: String = prop("BRIGHTCOVE_ACCOUNT")
  val BrightcovePlayer: String    = prop("BRIGHTCOVE_PLAYER")
}

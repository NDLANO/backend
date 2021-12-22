/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import no.ndla.network.{AuthUser, Domains}
import no.ndla.network.secrets.PropertyKeys

import scala.util.Properties._
import scala.util.{Failure, Success}

object FrontpageApiProperties {
  val IsKubernetes: Boolean = envOrNone("NDLA_IS_KUBERNETES").isDefined
  val Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")

  val ApplicationName = "frontpage-api"
  val ApplicationPort: Int = envOrElse("APPLICATION_PORT", "80").toInt
  val NumThreads: Int = propOrElse("NUM_THREADS", "200").toInt
  val DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  val ContactName: String = propOrElse("CONTACT_NAME", "NDLA")
  val ContactUrl: String = propOrElse("CONTACT_URL", "ndla.no")
  val ContactEmail: String = propOrElse("CONTACT_EMAIL", "support+api@ndla.no")
  val TermsUrl: String = propOrElse("TERMS_URL", "https://om.ndla.no/tos")
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val SecretsFile = "frontpage-api.secrets"

  lazy val MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  lazy val MetaServer: String = prop(PropertyKeys.MetaServerKey)
  lazy val MetaPort: Int = prop(PropertyKeys.MetaPortKey).toInt
  lazy val MetaSchema: String = prop(PropertyKeys.MetaSchemaKey)
  val MetaMaxConnections = 10

  lazy val Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))
  val RawImageApiUrl: String = s"$Domain/image-api/raw"

  val BrightcoveAccountId: String = prop("BRIGHTCOVE_ACCOUNT")
  val BrightcovePlayer: String = prop("BRIGHTCOVE_PLAYER")

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    propOrNone(key) match {
      case Some(prop) => prop
      case _          => default
    }
  }

}

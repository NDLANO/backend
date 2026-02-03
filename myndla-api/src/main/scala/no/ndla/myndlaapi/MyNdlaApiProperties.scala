/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.configuration.BaseProps
import no.ndla.database.DatabaseProps
import no.ndla.network.AuthUser

import scala.util.Properties.*

type Props = MyNdlaApiProperties

class MyNdlaApiProperties extends BaseProps with DatabaseProps {
  override def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  override def ApplicationName: String = "myndla-api"

  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def RedisHost: String = propOrElse("REDIS_HOST", "redis")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

  def nodeBBUrl: String = propOrElse("NODEBB_URL", s"$ApiGatewayUrl/groups")

  override def MetaMigrationLocation: String = "no/ndla/myndlaapi/db/migration"

  def emailDomain: String = Environment match {
    case "prod"  => "mail.ndla.no"
    case "local" => s"mail.test.ndla.no"
    case _       => s"mail.$Environment.ndla.no"
  }

  def outgoingEmailName: String      = propOrElse("NDLA_MYNDLA_EMAIL_NAME", "NDLA")
  def outgoingEmail: String          = propOrElse("NDLA_MYNDLA_EMAIL", s"noreply@$emailDomain")
  def MyNDLAContactEmail: String     = propOrElse("MYNDLA_CONTACT_EMAIL", "hjelp@ndla.no")
  def AWSEmailRegion: Option[String] = propOrNone("NDLA_AWS_EMAIL_REGION")
}

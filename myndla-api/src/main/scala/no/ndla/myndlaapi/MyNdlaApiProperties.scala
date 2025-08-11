/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.database.{DatabaseProps, HasDatabaseProps}
import no.ndla.network.AuthUser

import scala.util.Properties.*

trait Props extends HasBaseProps with HasDatabaseProps {
  lazy val props: MyNdlaApiProperties
}

class MyNdlaApiProperties extends BaseProps with DatabaseProps {
  override def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  override def ApplicationName: String = "myndla-api"

  def Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def RedisHost: String = propOrElse("REDIS_HOST", "redis")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

  def nodeBBUrl: String = propOrElse("NODEBB_URL", s"$ApiGatewayUrl/groups")

  override def MetaMigrationLocation: String = "no/ndla/myndlaapi/db/migration"
}

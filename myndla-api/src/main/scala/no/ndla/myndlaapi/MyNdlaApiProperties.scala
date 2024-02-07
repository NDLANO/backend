/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi

import no.ndla.common.Environment.prop
import no.ndla.common.configuration.{BaseProps, HasBaseProps}
import no.ndla.common.secrets.PropertyKeys
import no.ndla.network.AuthUser

import scala.util.Properties._

trait Props extends HasBaseProps {
  val props: MyNdlaApiProperties
}

class MyNdlaApiProperties extends BaseProps {
  override def ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt
  override def ApplicationName: String = "myndla-api"

  def Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  def MetaUserName: String    = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String    = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String    = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String      = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int           = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String      = prop(PropertyKeys.MetaSchemaKey)
  def MetaMaxConnections: Int = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt

  def RedisHost: String = propOrElse("REDIS_HOST", "redis")
  def RedisPort: Int    = propOrElse("REDIS_PORT", "6379").toInt

  def nodeBBUrl: String = propOrElse("NODEBB_URL", s"$ApiGatewayUrl/groups")

  // NOTE: These props are for migrating the learningpath-api data to this api
  //       and should be removed when the migration is done.
  def LpMetaUserName: String    = prop("LP_" + PropertyKeys.MetaUserNameKey)
  def LpMetaPassword: String    = prop("LP_" + PropertyKeys.MetaPasswordKey)
  def LpMetaResource: String    = prop("LP_" + PropertyKeys.MetaResourceKey)
  def LpMetaServer: String      = prop("LP_" + PropertyKeys.MetaServerKey)
  def LpMetaPort: Int           = prop("LP_" + PropertyKeys.MetaPortKey).toInt
  def LpMetaSchema: String      = prop("LP_" + PropertyKeys.MetaSchemaKey)
  def LpMetaMaxConnections: Int = propOrElse("LP_" + PropertyKeys.MetaMaxConnections, "10").toInt

  def migrateToLocalDB: Boolean = propOrNone("LP_MIGRATE").exists(x => x.toLowerCase == "true")
}

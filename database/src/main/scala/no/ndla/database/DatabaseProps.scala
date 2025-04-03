/*
 * Part of NDLA database
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import no.ndla.common.configuration.{BaseProps, Prop}
import no.ndla.common.secrets.PropertyKeys

import scala.util.Properties.propOrElse

trait DatabaseProps {
  this: BaseProps =>
  val MetaUserName: Prop[String] = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword: Prop[String] = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource: Prop[String] = prop(PropertyKeys.MetaResourceKey)
  val MetaServer: Prop[String]   = prop(PropertyKeys.MetaServerKey)
  val MetaPort: Prop[Int]        = prop(PropertyKeys.MetaPortKey).flatMap(_.toIntOption)
  val MetaSchema: Prop[String]   = prop(PropertyKeys.MetaSchemaKey)
  val MetaMaxConnections: Int    = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt

  def MetaMigrationLocation: String
  def MetaMigrationTable: Option[String] = None
}

trait HasDatabaseProps {
  val props: DatabaseProps
}

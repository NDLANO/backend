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

abstract class DatabaseProps(using baseProps: BaseProps) {
  val MetaUserName: Prop[String] = baseProps.prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword: Prop[String] = baseProps.prop(PropertyKeys.MetaPasswordKey)
  val MetaResource: Prop[String] = baseProps.prop(PropertyKeys.MetaResourceKey)
  val MetaServer: Prop[String]   = baseProps.prop(PropertyKeys.MetaServerKey)
  val MetaPort: Prop[Int]        = baseProps.propMap(baseProps.prop(PropertyKeys.MetaPortKey))(_.toInt)
  val MetaSchema: Prop[String]   = baseProps.prop(PropertyKeys.MetaSchemaKey)
  val MetaMaxConnections: Int    = propOrElse(PropertyKeys.MetaMaxConnections, "10").toInt

  def MetaMigrationLocation: String
  def MetaMigrationTable: Option[String] = None
}

trait HasDatabaseProps {
  lazy val props: DatabaseProps
}

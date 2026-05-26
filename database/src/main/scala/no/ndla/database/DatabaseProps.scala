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

/** Every `META_*` value is read first from a per-app prefixed env var (e.g. `ARTICLE_API_META_RESOURCE`) and only then
  * from the shared unprefixed env var (`META_RESOURCE`). This lets a single JVM running every Scala *-api together
  * (monolith mode) point each app at its own database/schema while keeping the existing unprefixed env vars working
  * unchanged in microservice deployments. `MetaSchema` additionally falls back to the [[BaseProps.ApplicationName]]
  * normalised to a SQL-safe identifier (e.g. `article_api`) so a monolith dev setup needs no schema config at all.
  */
trait DatabaseProps extends BaseProps {
  val MetaUserName: Prop[String] = prefixedProp(PropertyKeys.MetaUserNameKey)
  val MetaPassword: Prop[String] = prefixedProp(PropertyKeys.MetaPasswordKey)
  val MetaResource: Prop[String] = prefixedProp(PropertyKeys.MetaResourceKey)
  val MetaServer: Prop[String]   = prefixedProp(PropertyKeys.MetaServerKey)
  val MetaPort: Prop[Int]        = propMap(prefixedProp(PropertyKeys.MetaPortKey))(_.toInt)
  val MetaSchema: Prop[String]   = prefixedPropOrElse(PropertyKeys.MetaSchemaKey, ApplicationName.replace("-", "_"))
  val MetaMaxConnections: Int    = prefixedIntPropOrElse(PropertyKeys.MetaMaxConnections, 10)

  def MetaMigrationLocation: String
  def MetaMigrationTable: Option[String] = None
}

trait HasDatabaseProps {
  lazy val props: DatabaseProps
}

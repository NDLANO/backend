/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import no.ndla.common.model.domain.config.ConfigMeta
import no.ndla.myndlaapi.Props
import scalikejdbc.SQLSyntaxSupport

class DBConfigMeta(using props: Props) extends SQLSyntaxSupport[ConfigMeta] {
  override val tableName: String          = "configtable"
  override val schemaName: Option[String] = Some(props.MetaSchema)
}

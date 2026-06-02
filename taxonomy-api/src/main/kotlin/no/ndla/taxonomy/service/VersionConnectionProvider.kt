/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** Updates the Connection object with the correct schema, based on the tenant identifier */
@Component
class VersionConnectionProvider(private val dataSource: DataSource) :
    AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String>() {

  @Value("\${spring.datasource.hikari.schema:taxonomy_api}")
  private lateinit var defaultSchema: String

  protected override fun selectAnyDataSource(): DataSource = dataSource

  protected override fun selectDataSource(versionSchemaName: String?): DataSource = dataSource

  @Throws(SQLException::class)
  override fun getConnection(versionSchemaName: String?): Connection? {
    val connection = anyConnection
    connection.schema = versionSchemaName
    return connection
  }

  @Throws(SQLException::class)
  override fun releaseConnection(tenantIdentifier: String?, connection: Connection?) {
    connection?.let {
      it.schema = defaultSchema
      it.close()
    }
  }
}

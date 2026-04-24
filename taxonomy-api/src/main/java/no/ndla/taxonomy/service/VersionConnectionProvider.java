/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Updates the Connection object with the correct schema, based on the tenant identifier
 */
@Component
public class VersionConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {
    private final DataSource dataSource;

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    @Autowired
    public VersionConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSource;
    }

    @Override
    protected DataSource selectDataSource(String versionSchemaName) {
        return dataSource;
    }

    @Override
    public Connection getConnection(String versionSchemaName) throws SQLException {
        final Connection connection = getAnyConnection();
        connection.setSchema(versionSchemaName);
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.setSchema(defaultSchema);
        connection.close();
    }
}

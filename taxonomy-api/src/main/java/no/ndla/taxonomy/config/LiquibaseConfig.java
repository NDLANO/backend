/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class LiquibaseConfig implements InitializingBean, ResourceLoaderAware {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    @Value("${spring.datasource.url:}")
    private String dataSourceUrl;

    @Value("${spring.datasource.username:}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password:}")
    private String dataSourcePassword;

    @Autowired
    private LiquibaseProperties liquibaseProperties;

    private ResourceLoader resourceLoader;

    @Override
    public void afterPropertiesSet() {
        try {
            List<String> schemas = new ArrayList<>();
            ResultSet resultSet = dataSource
                    .getConnection()
                    .prepareStatement("SELECT hash FROM version")
                    .executeQuery();
            while (resultSet.next()) {
                schemas.add(String.format("%s_%s", defaultSchema, resultSet.getString(1)));
            }
            this.runOnAllSchemas(dataSource, schemas);
        } catch (SQLException | LiquibaseException exception) {
            // No version table
            logger.info(
                    "Failed to find version table in database. Does not run migration on alternative schemas",
                    exception);
        }
    }

    protected void runOnAllSchemas(DataSource dataSource, Collection<String> schemas) throws LiquibaseException {
        for (String schema : schemas) {
            logger.info("Initializing Liquibase for version " + schema);

            // TODO: There is probably a better way to do this.
            var ds = dataSource;
            if (!"".equals(dataSourceUsername)) {
                var hc = new HikariConfig();
                hc.setMaximumPoolSize(1);
                hc.setSchema(schema);
                hc.setUsername(dataSourceUsername);
                hc.setPassword(dataSourcePassword);
                hc.setJdbcUrl(dataSourceUrl);
                ds = new HikariDataSource(hc);
            }

            SpringLiquibase liquibase = this.getSpringLiquibase(ds, schema);
            liquibase.afterPropertiesSet();
            logger.info("Liquibase ran for version " + schema);
        }
    }

    protected SpringLiquibase getSpringLiquibase(DataSource dataSource, String schema) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setResourceLoader(getResourceLoader());
        liquibase.setDataSource(dataSource);
        liquibase.setDefaultSchema(schema);
        liquibase.setLiquibaseSchema(schema);
        liquibase.setChangeLogParameters(Map.of("schema", schema));
        liquibase.setChangeLog(liquibaseProperties.getChangeLog());
        return liquibase;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}

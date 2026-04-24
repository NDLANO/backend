/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@DirtiesContext
public class AbstractIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgresDB = new PostgreSQLContainer("postgres:17.5");

    @Autowired
    EntityManager entityManager;

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresDB::getJdbcUrl);
        registry.add("spring.datasource.username", postgresDB::getUsername);
        registry.add("spring.datasource.password", postgresDB::getPassword);
        registry.add("spring.datasource.hikari.schema", () -> "taxonomy_api");
    }

    protected boolean checkSchemaExists(String schemaName) {
        Object result = entityManager
                .createNativeQuery(String.format(
                        "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '%s'", schemaName))
                .getSingleResult();
        return result.equals(schemaName);
    }
}

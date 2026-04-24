/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.rest.v1.commands.VersionPostPut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Will only be run in maven using '-P integration'
 */
@SpringBootTest
@Transactional
public class VersionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    VersionRepository versionRepository;

    @Autowired
    VersionService versionService;

    @BeforeEach
    void clearAllRepos() {
        versionRepository.deleteAllAndFlush();
    }

    @Test
    void can_create_new_version_with_own_schema() {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version version = versionService.createNewVersion(Optional.empty(), command);
        assertEquals(VersionType.BETA, version.getVersionType());

        // Check that specified schema exists
        assertTrue(checkSchemaExists(versionService.schemaFromHash(version.getHash())));
    }

    @Test
    public void can_publish_version() {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version version = versionService.createNewVersion(Optional.empty(), command);
        versionService.publishBetaAndArchiveCurrent(version.getPublicId());

        Version published = versionRepository.getByPublicId(version.getPublicId());
        assertEquals(VersionType.PUBLISHED, published.getVersionType());
        assertNotNull(published.getPublished());
    }
}

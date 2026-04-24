/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VersionTest {
    private Version version;

    @BeforeEach
    public void setUp() {
        version = new Version();
    }

    @Test
    public void has_public_id() {
        assertNotNull(version.getPublicId());
        assertTrue(version.getPublicId().toString().startsWith("urn:version:"));
    }

    @Test
    public void version_type_can_be_changed() {
        assertEquals(version.getVersionType(), VersionType.BETA);
        version.setVersionType(VersionType.PUBLISHED);
        assertEquals(version.getVersionType(), VersionType.PUBLISHED);
    }
}

/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAllTrue;
import static no.ndla.taxonomy.TestUtils.getId;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.rest.v1.commands.VersionPostPut;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class VersionsTest extends RestTest {

    @BeforeEach
    void cleanDatabase() {
        versionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_all_versions() throws Exception {
        builder.version();
        builder.version();

        MockHttpServletResponse response = testUtils.getResource("/v1/versions");
        VersionDTO[] versions = testUtils.getObject(VersionDTO[].class, response);
        assertEquals(2, versions.length);
    }

    @Test
    public void can_get_specified_version() throws Exception {
        URI versionId = URI.create("urn:version:1");
        builder.version(v -> v.publicId(versionId));

        {
            MockHttpServletResponse response = testUtils.getResource("/v1/versions/" + versionId);
            VersionDTO version = testUtils.getObject(VersionDTO.class, response);
            assertEquals(versionId, version.getId());
            assertNotNull(version.getHash());
            assertEquals(Optional.empty(), version.getPublished());
            assertEquals(Optional.empty(), version.getArchived());
        }
        {
            MockHttpServletResponse response =
                    testUtils.getResource("/v1/versions/urn:version:2", status().is4xxClientError());
            assertEquals(404, response.getStatus());
            assertEquals("{\"error\":\"Version not found\"}", response.getContentAsString());
        }
    }

    @Test
    public void can_get_versions_from_hash() throws Exception {
        Version version = builder.version();
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/versions?hash=" + version.getHash());
            VersionDTO[] versions = testUtils.getObject(VersionDTO[].class, response);
            assertEquals(1, versions.length);
            assertEquals(version.getHash(), versions[0].getHash());
        }
        {
            MockHttpServletResponse response =
                    testUtils.getResource("/v1/versions?hash=random", status().is4xxClientError());
            assertEquals(404, response.getStatus());
            assertEquals("{\"error\":\"Version not found\"}", response.getContentAsString());
        }
    }

    @Test
    public void can_get_versions_of_type() throws Exception {
        Version version = builder.version(); // BETA
        MockHttpServletResponse response = testUtils.getResource("/v1/versions?type=BETA");
        VersionDTO[] versions = testUtils.getObject(VersionDTO[].class, response);
        assertEquals(1, versions.length);
        assertAllTrue(versions, v -> v.getVersionType() == VersionType.BETA);
        assertAllTrue(versions, v -> v.getId().equals(version.getPublicId()));

        Version version2 = builder.version(v -> v.type(VersionType.PUBLISHED));
        MockHttpServletResponse response2 = testUtils.getResource("/v1/versions?type=PUBLISHED");
        VersionDTO[] versions2 = testUtils.getObject(VersionDTO[].class, response2);
        assertEquals(1, versions2.length);
        assertAllTrue(versions2, v -> v.getVersionType() == VersionType.PUBLISHED);
        assertAllTrue(versions2, v -> v.getId().equals(version2.getPublicId()));
    }

    @Test
    public void can_create_version() throws Exception {
        final var createVersionCommand = new VersionPostPut() {
            {
                id = Optional.of(URI.create("urn:version:1"));
                name = "Beta";
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/versions", createVersionCommand);
        URI id = getId(response);

        Version version = versionRepository.getByPublicId(id);
        assertEquals(createVersionCommand.id.get(), version.getPublicId());
        assertEquals(VersionType.BETA, version.getVersionType());
        assertEquals("Beta", version.getName());
    }

    @Test
    public void can_create_version_based_on_existing() throws Exception {
        Version published = builder.version(v -> v.type(VersionType.PUBLISHED));
        final var createVersionCommand = new VersionPostPut() {
            {
                id = Optional.of(URI.create("urn:version:1"));
                name = "Beta";
            }
        };

        MockHttpServletResponse response =
                testUtils.createResource("/v1/versions?sourceId=" + published.getPublicId(), createVersionCommand);
        URI id = getId(response);

        Version version = versionRepository.getByPublicId(id);
        assertEquals(createVersionCommand.id.get(), version.getPublicId());
        assertEquals(VersionType.BETA, version.getVersionType());
        assertEquals("Beta", version.getName());
    }

    @Test
    public void can_delete_version() throws Exception {
        Version version = builder.version(); // BETA
        testUtils.deleteResource("/v1/versions/" + version.getPublicId());
        try {
            versionRepository.getByPublicId(version.getPublicId());
            fail("Failed to delete version");
        } catch (Exception nfe) {
            // All OK
        }
    }

    @Test
    public void cannot_delete_locked_version() throws Exception {
        Version locked = builder.version(v -> v.locked(true));
        {
            MockHttpServletResponse response =
                    testUtils.deleteResource("/v1/versions/" + locked.getPublicId(), status().is4xxClientError());
            assertEquals(400, response.getStatus());
            assertEquals("{\"error\":\"Cannot delete locked version\"}", response.getContentAsString());
        }
    }

    @Test
    public void can_update_version() throws Exception {
        Version version = builder.version(); // BETA
        URI newUri = URI.create("urn:version:1");
        final var updateVersionCommand = new VersionPostPut() {
            {
                id = Optional.of(newUri);
                name = "New name";
                locked = Optional.of(true);
            }
        };

        testUtils.updateResource("/v1/versions/" + version.getPublicId(), updateVersionCommand);

        Version updated = versionRepository.getByPublicId(newUri);
        assertEquals(updateVersionCommand.id.get(), updated.getPublicId());
        assertEquals(VersionType.BETA, updated.getVersionType());
        assertEquals("New name", updated.getName());
        assertTrue(updated.isLocked());
        assertEquals(version.getHash(), updated.getHash()); // Not changed during update
    }

    @Test
    public void can_publish_version() throws Exception {
        var versionUri = URI.create("urn:version:beta");
        final var createVersionCommand = new VersionPostPut() {
            {
                id = Optional.of(versionUri);
                name = "Beta";
            }
        };
        testUtils.createResource("/v1/versions", createVersionCommand);
        testUtils.updateResource("/v1/versions/" + versionUri + "/publish", null);

        Version updated = versionRepository.getByPublicId(versionUri);
        assertEquals(VersionType.PUBLISHED, updated.getVersionType());
        assertTrue(updated.isLocked());
        assertNotNull(updated.getPublished());
    }

    @Test
    public void cannot_publish_published_or_archived_version() throws Exception {
        Version version = builder.version(v -> v.type(VersionType.PUBLISHED));
        MockHttpServletResponse response = testUtils.updateResource(
                "/v1/versions/" + version.getPublicId() + "/publish", null, status().is4xxClientError());
        assertEquals(400, response.getStatus());
        assertEquals("{\"error\":\"Version has wrong type\"}", response.getContentAsString());

        Version version2 = builder.version(v -> v.type(VersionType.ARCHIVED));
        MockHttpServletResponse response2 = testUtils.updateResource(
                "/v1/versions/" + version2.getPublicId() + "/publish", null, status().is4xxClientError());
        assertEquals(400, response2.getStatus());
        assertEquals("{\"error\":\"Version has wrong type\"}", response2.getContentAsString());
    }

    @Test
    public void publishing_version_unpublishes_current() throws Exception {
        Version published = builder.version(v -> v.type(VersionType.PUBLISHED));
        Version beta = builder.version();
        testUtils.updateResource("/v1/versions/" + beta.getPublicId() + "/publish", null);

        Version updated = versionRepository.getByPublicId(beta.getPublicId());
        assertEquals(VersionType.PUBLISHED, updated.getVersionType());
        assertNotNull(updated.getPublished());

        Version unpublished = versionRepository.getByPublicId(published.getPublicId());
        assertEquals(VersionType.ARCHIVED, unpublished.getVersionType());
        assertNotNull(unpublished.getArchived());
    }
}

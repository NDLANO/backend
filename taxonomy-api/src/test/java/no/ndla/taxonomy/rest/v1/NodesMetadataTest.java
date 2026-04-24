/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.JsonGrepCode;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.service.dtos.MetadataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class NodesMetadataTest extends RestTest {

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    void get_metadata_for_missing_node_fails() throws Exception {
        testUtils.getResource("/v1/nodes/urn:node:1/metadata", status().isNotFound());
    }

    @Test
    void can_get_metadata_for_node() throws Exception {
        URI publicId = builder.node().getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/" + publicId + "/metadata");
        final var metadata = testUtils.getObject(MetadataDTO.class, response);

        assertNotNull(metadata);
        assertTrue(metadata.isVisible());
    }

    @Test
    void can_update_metadata_for_node() throws Exception {
        URI publicId = builder.node().getPublicId();

        testUtils.updateResource(
                "/v1/nodes/" + publicId + "/metadata",
                new MetadataDTO() {
                    {
                        visible = false;
                        grepCodes = Set.of("KM123");
                        customFields = Map.of("key", "value");
                    }
                },
                status().isOk());

        Node node = nodeRepository.getByPublicId(publicId);
        assertNotNull(node.getMetadata());
        assertFalse(node.getMetadata().isVisible());
        Set<String> codes = node.getMetadata().getGrepCodes().stream()
                .map(JsonGrepCode::getCode)
                .collect(Collectors.toSet());
        assertTrue(codes.contains("KM123"));
        var customFieldValues = node.getMetadata().getCustomFields();
        assertTrue(customFieldValues.containsKey("key"));
        assertTrue(customFieldValues.containsValue("value"));
    }

    @Test
    void can_remove_metadata_for_node() throws Exception {
        URI publicId = builder.node(n -> n.customField("key", "value").grepCode("KM123"))
                .getPublicId();

        testUtils.updateResource(
                "/v1/nodes/" + publicId + "/metadata",
                new MetadataDTO() {
                    {
                        visible = true;
                        grepCodes = Set.of();
                        customFields = Map.of();
                    }
                },
                status().isOk());

        Node node = nodeRepository.getByPublicId(publicId);
        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
        assertTrue(node.getMetadata().getGrepCodes().isEmpty());
        assertTrue(node.getCustomFields().isEmpty());
    }
}

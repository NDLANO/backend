/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourceTypesTest extends RestTest {

    @Test
    public void can_get_all_resource_types() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types");
        ResourceTypeDTO[] resourceTypes = testUtils.getObject(ResourceTypeDTO[].class, response);
        assertEquals(ResourceType.values().length, resourceTypes.length);
        assertAllTrue(resourceTypes, s -> isValidId(s.getId()));
    }

    @Test
    public void can_get_resourcetype_by_id() throws Exception {
        URI id = ResourceType.SUBJECT_MATERIAL.getPublicId();
        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types/" + id);
        ResourceTypeDTO resourceType = testUtils.getObject(ResourceTypeDTO.class, response);
        assertEquals(id, resourceType.getId());
    }

    @Test
    public void can_get_all_resource_types_with_translation() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types?language=nb");
        ResourceTypeDTO[] resourceTypes = testUtils.getObject(ResourceTypeDTO[].class, response);
        assertAnyTrue(resourceTypes, s -> "Fagstoff".equals(s.getName()));
        assertAnyTrue(resourceTypes, s -> "Oppgave".equals(s.getName()));
    }

    @Test
    public void can_get_resourcetype_by_id_with_translation() throws Exception {
        URI id = ResourceType.SUBJECT_MATERIAL.getPublicId();
        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types/" + id + "?language=nb");
        ResourceTypeDTO resourceType = testUtils.getObject(ResourceTypeDTO.class, response);
        assertEquals("Fagstoff", resourceType.getName());
    }

    @Test
    public void unknown_resourcetype_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/resource-types/doesnotexist", status().isNotFound());
    }
}

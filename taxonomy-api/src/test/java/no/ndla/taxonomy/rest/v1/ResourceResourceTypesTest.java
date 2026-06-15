/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static no.ndla.taxonomy.TestUtils.getId;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypeDTO;
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypePOST;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourceResourceTypesTest extends RestTest {

    @Test
    public void can_add_resourcetype_to_resource() throws Exception {
        var createdResource = newResource();
        createdResource.setName("Introduction to integration");
        var integrationResourceId = createdResource.getPublicId();

        ResourceType rt = ResourceType.SUBJECT_MATERIAL;
        URI textTypeId = rt.getPublicId();

        URI id = getId(testUtils.createResource(
                "/v1/resource-resourcetypes", new ResourceResourceTypePOST(integrationResourceId, textTypeId)));

        var resource = nodeRepository.getByPublicId(integrationResourceId);
        assertEquals(1, resource.getResourceTypes().size());
        assertAnyTrue(resource.getResourceTypes(), t -> "Fagstoff".equals(t.getTranslatedName("nb")));
        assertNotNull(id);
    }

    @Test
    public void cannot_have_duplicate_resourcetypes_for_resource() throws Exception {
        var integrationResource = newResource();
        integrationResource.setName("Introduction to integration");

        ResourceType resourceType = ResourceType.SUBJECT_MATERIAL;
        integrationResource.addResourceType(resourceType);
        save(integrationResource);

        testUtils.createResource(
                "/v1/resource-resourcetypes",
                new ResourceResourceTypePOST(integrationResource.getPublicId(), resourceType.getPublicId()),
                status().isConflict());
    }

    @Test
    public void can_delete_resource_resourcetype() throws Exception {
        var integrationResource = builder.node(NodeType.RESOURCE, r -> r.name("Introduction to integration"));
        integrationResource.addResourceType(ResourceType.DOCUMENTARY);
        save(integrationResource);

        testUtils.deleteResource("/v1/resource-resourcetypes/" + "urn:resource-resourcetype:"
                + integrationResource.getPublicId() + "_" + ResourceType.DOCUMENTARY.getPublicId());
        var node = nodeRepository.getByPublicId(integrationResource.getPublicId());
        assertEquals(0, node.getResourceTypes().size());
    }

    @Test
    public void can_list_all_resource_resourcetypes() throws Exception {
        var trigonometry = newResource();
        trigonometry.setName("Advanced trigonometry");
        trigonometry.addResourceType(ResourceType.DOCUMENTARY);
        save(trigonometry);

        var integration = newResource();
        integration.setName("Introduction to integration");
        integration.addResourceType(ResourceType.REVIEW_RESOURCE);
        save(integration);

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-resourcetypes");
        ResourceResourceTypeDTO[] resourceResourcetypes =
                testUtils.getObject(ResourceResourceTypeDTO[].class, response);
        assertEquals(2, resourceResourcetypes.length);
        assertAnyTrue(
                resourceResourcetypes,
                t -> trigonometry.getPublicId().equals(t.getResourceId())
                        && ResourceType.DOCUMENTARY.getPublicId().equals(t.getResourceTypeId()));
        assertAnyTrue(
                resourceResourcetypes,
                t -> integration.getPublicId().equals(t.getResourceId())
                        && ResourceType.REVIEW_RESOURCE.getPublicId().equals(t.getResourceTypeId()));
    }

    @Test
    public void can_get_a_resource_resourcetype() throws Exception {
        var resource = newResource();
        resource.setName("Advanced trigonometry");
        var resourceType = ResourceType.SUBJECT_MATERIAL;
        resource.addResourceType(resourceType);
        save(resource);

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-resourcetypes/"
                + "urn:resource-resourcetype:" + resource.getPublicId() + "_" + resourceType.getPublicId());
        ResourceResourceTypeDTO result = testUtils.getObject(ResourceResourceTypeDTO.class, response);

        assertEquals(resource.getPublicId(), result.getResourceId());
        assertEquals(resourceType.getPublicId(), result.getResourceTypeId());
    }
}

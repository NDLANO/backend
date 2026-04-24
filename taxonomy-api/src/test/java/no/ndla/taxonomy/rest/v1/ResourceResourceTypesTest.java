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
import no.ndla.taxonomy.domain.ResourceResourceType;
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

        URI textTypeId = newResourceType().name("text").getPublicId();

        URI id = getId(testUtils.createResource("/v1/resource-resourcetypes", new ResourceResourceTypePOST() {
            {
                resourceId = integrationResourceId;
                resourceTypeId = textTypeId;
            }
        }));

        var resource = nodeRepository.getByPublicId(integrationResourceId);
        assertEquals(1, resource.getResourceTypes().size());
        assertAnyTrue(resource.getResourceTypes(), t -> "text".equals(t.getName()));
        assertNotNull(resourceResourceTypeRepository.getByPublicId(id));
    }

    @Test
    public void cannot_have_duplicate_resourcetypes_for_resource() throws Exception {
        var integrationResource = newResource();
        integrationResource.setName("Introduction to integration");

        ResourceType resourceType = newResourceType().name("text");
        save(integrationResource.addResourceType(resourceType));

        testUtils.createResource(
                "/v1/resource-resourcetypes",
                new ResourceResourceTypePOST() {
                    {
                        resourceId = integrationResource.getPublicId();
                        resourceTypeId = resourceType.getPublicId();
                    }
                },
                status().isConflict());
    }

    @Test
    public void can_delete_resource_resourcetype() throws Exception {
        var integrationResource = builder.node(NodeType.RESOURCE, r -> r.name("Introduction to integration"));
        ResourceType resourceType = builder.resourceType(rt -> rt.name("text"));
        URI id = save(integrationResource.addResourceType(resourceType)).getPublicId();

        testUtils.deleteResource("/v1/resource-resourcetypes/" + id);
        assertNull(resourceResourceTypeRepository.findByPublicId(id));
    }

    @Test
    public void can_list_all_resource_resourcetypes() throws Exception {
        var trigonometry = newResource();
        trigonometry.setName("Advanced trigonometry");
        ResourceType article = newResourceType().name("article");
        save(trigonometry.addResourceType(article));

        var integration = newResource();
        integration.setName("Introduction to integration");
        ResourceType text = newResourceType().name("text");
        save(integration.addResourceType(text));

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-resourcetypes");
        ResourceResourceTypeDTO[] resourceResourcetypes =
                testUtils.getObject(ResourceResourceTypeDTO[].class, response);
        assertEquals(2, resourceResourcetypes.length);
        assertAnyTrue(
                resourceResourcetypes,
                t -> trigonometry.getPublicId().equals(t.resourceId)
                        && article.getPublicId().equals(t.resourceTypeId));
        assertAnyTrue(
                resourceResourcetypes,
                t -> integration.getPublicId().equals(t.resourceId)
                        && text.getPublicId().equals(t.resourceTypeId));
    }

    @Test
    public void can_get_a_resource_resourcetype() throws Exception {
        var resource = newResource();
        resource.setName("Advanced trigonometry");
        ResourceType resourceType = newResourceType().name("article");
        ResourceResourceType resourceResourceType = resource.addResourceType(resourceType);
        URI id = save(resourceResourceType).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-resourcetypes/" + id);
        ResourceResourceTypeDTO result = testUtils.getObject(ResourceResourceTypeDTO.class, response);

        assertEquals(resource.getPublicId(), result.resourceId);
        assertEquals(resourceType.getPublicId(), result.resourceTypeId);
    }

    @Test
    public void can_change_id_of_resource_type() throws Exception {
        var resource = newResource();
        resource.setName("Advanced trigonometry");
        ResourceType resourceType = newResourceType().name("article");
        ResourceResourceType resourceResourceType = resource.addResourceType(resourceType);
        URI id = save(resourceResourceType).getPublicId();

        resourceType.setPublicId(URI.create("urn:resourcetype:article"));

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-resourcetypes/" + id);
        ResourceResourceTypeDTO result = testUtils.getObject(ResourceResourceTypeDTO.class, response);

        assertEquals(resource.getPublicId(), result.resourceId);
        assertEquals(URI.create("urn:resourcetype:article"), result.resourceTypeId);
    }
}

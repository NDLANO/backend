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
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypePUT;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourceTypesTest extends RestTest {

    @Test
    public void can_get_all_resource_types() throws Exception {
        builder.resourceType(rt -> rt.name("video").subtype(st -> st.name("lecture")));
        builder.resourceType(rt -> rt.name("audio"));

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types");
        ResourceTypeDTO[] resourceTypes = testUtils.getObject(ResourceTypeDTO[].class, response);

        assertTrue(resourceTypes.length >= 2);
        assertAnyTrue(
                resourceTypes,
                s -> "video".equals(s.name) && "lecture".equals(s.subtypes.get().getFirst().name));
        assertAnyTrue(resourceTypes, s -> "audio".equals(s.name));
        assertAllTrue(resourceTypes, s -> isValidId(s.id));
    }

    @Test
    public void can_get_resourcetype_by_id() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types/" + id.toString());
        ResourceTypeDTO resourceType = testUtils.getObject(ResourceTypeDTO.class, response);
        assertEquals(id, resourceType.id);
        assertTrue(resourceType.subtypes.isEmpty());
    }

    @Test
    public void can_get_all_resource_types_with_translation() throws Exception {
        builder.resourceType(rt -> rt.name("video").translation("nb", tr -> tr.name("film")));
        builder.resourceType(rt -> rt.name("audio").translation("nb", tr -> tr.name("lydklipp")));

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types?language=nb");
        ResourceTypeDTO[] resourcetypes = testUtils.getObject(ResourceTypeDTO[].class, response);

        assertTrue(resourcetypes.length >= 2);
        assertAnyTrue(resourcetypes, s -> "film".equals(s.name));
        assertAnyTrue(resourcetypes, s -> "lydklipp".equals(s.name));
    }

    @Test
    public void can_get_resourcetype_by_id_with_translation() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video").translation("nb", tr -> tr.name("film")))
                .getPublicId();

        MockHttpServletResponse response =
                testUtils.getResource("/v1/resource-types/" + id.toString() + "?language=nb");
        ResourceTypeDTO resourceType = testUtils.getObject(ResourceTypeDTO.class, response);
        assertEquals("film", resourceType.name);
    }

    @Test
    public void unknown_resourcetype_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/resource-types/doesnotexist", status().isNotFound());
    }

    @Test
    public void can_create_resourcetype() throws Exception {
        ResourceTypePUT command = new ResourceTypePUT() {
            {
                id = URI.create("urn:resourcetype:1");
                name = "name";
            }
        };

        testUtils.createResource("/v1/resource-types", command);

        ResourceType result = resourceTypeRepository.getByPublicId(command.id);
        assertEquals(command.name, result.getName());
    }

    @Test
    public void cannot_create_duplicate_resourcetype() throws Exception {
        ResourceTypePUT command = new ResourceTypePUT() {
            {
                id = URI.create("urn:resourcetype:1");
                name = "name";
            }
        };
        testUtils.createResource("/v1/resource-types", command);
        testUtils.createResource("/v1/resource-types", command, status().isConflict());
    }

    @Test
    public void can_delete_resourcetype() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();
        testUtils.deleteResource("/v1/resource-types/" + id);
        assertNull(resourceTypeRepository.findByPublicId(id));
    }

    @Test
    public void can_update_resourcetype() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();

        testUtils.updateResource("/v1/resource-types/" + id, new ResourceTypePUT() {
            {
                name = "Audiovideo";
            }
        });

        ResourceType result = resourceTypeRepository.getByPublicId(id);
        assertEquals("Audiovideo", result.getName());
    }

    @Test
    public void can_change_resource_type_id() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();

        testUtils.updateResource("/v1/resource-types/" + id, new ResourceTypePUT() {
            {
                name = "Audiovideo";
                id = URI.create("urn:resourcetype:audiovideo");
            }
        });

        ResourceType result = resourceTypeRepository.getByPublicId(URI.create("urn:resourcetype:audiovideo"));
        assertEquals("urn:resourcetype:audiovideo", result.getPublicId().toString());
    }

    @Test
    public void can_add_subresourcetype_to_resourcetype() throws Exception {
        ResourceType parent = builder.resourceType(rt -> rt.name("external"));

        URI childId = getId(testUtils.createResource("/v1/resource-types", new ResourceTypePUT() {
            {
                parentId = parent.getPublicId();
                name = "youtube";
            }
        }));

        ResourceType child = resourceTypeRepository.getByPublicId(childId);
        assertEquals(parent.getPublicId(), child.getParent().get().getPublicId());
    }

    @Test
    public void can_remove_parent_from_resourcetype() throws Exception {
        ResourceType parent = builder.resourceType(rt -> rt.name("external"));
        ResourceType child = builder.resourceType(rt -> rt.name("youtube"));
        child.setParent(parent);

        URI childId = child.getPublicId();

        testUtils.updateResource("/v1/resource-types/" + childId, new ResourceTypePUT() {
            {
                parentId = null;
                name = child.getName();
            }
        });

        assertFalse(resourceTypeRepository.getByPublicId(childId).getParent().isPresent());
    }

    @Test
    public void can_update_parent_resourcetype() throws Exception {
        ResourceType oldParent = builder.resourceType(rt -> rt.name("external"));
        ResourceType child = builder.resourceType(rt -> rt.name("youtube"));
        child.setParent(oldParent);
        URI newParentId = builder.resourceType(rt -> rt.name("video")).getPublicId();

        testUtils.updateResource("/v1/resource-types/" + child.getPublicId(), new ResourceTypePUT() {
            {
                parentId = newParentId;
                name = child.getName();
            }
        });

        assertEquals("video", child.getParent().get().getName());
    }

    @Test
    public void can_get_subtypes() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("external")
                        .subtype(st -> st.name("youtube"))
                        .subtype(st -> st.name("ted"))
                        .subtype(st -> st.name("vimeo")))
                .getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types/" + id + "/subtypes");
        ResourceTypeDTO[] subResourceTypes = testUtils.getObject(ResourceTypeDTO[].class, response);

        assertEquals(3, subResourceTypes.length);
        assertAnyTrue(subResourceTypes, t -> "youtube".equals(t.name));
        assertAnyTrue(subResourceTypes, t -> "ted".equals(t.name));
        assertAnyTrue(subResourceTypes, t -> "vimeo".equals(t.name));
        assertAllTrue(subResourceTypes, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subtypes_recursively() throws Exception {
        URI id = builder.resourceType(
                        rt -> rt.name("external").subtype(st -> st.name("video").subtype(st2 -> st2.name("youtube"))))
                .getPublicId();

        MockHttpServletResponse response =
                testUtils.getResource("/v1/resource-types/" + id + "/subtypes?recursive=true");
        ResourceTypeDTO[] subResourceTypes = testUtils.getObject(ResourceTypeDTO[].class, response);

        assertEquals(1, subResourceTypes.length);
        assertEquals("video", subResourceTypes[0].name);
        assertEquals("youtube", subResourceTypes[0].subtypes.get().getFirst().name);
    }

    @Test
    void can_update_order_of_resourcetypes() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();

        testUtils.updateResource("/v1/resource-types/" + id, new ResourceTypePUT() {
            {
                order = 0;
            }
        });

        ResourceType result = resourceTypeRepository.getByPublicId(id);
        assertEquals(0, result.getOrder());

        var all = resourceTypeRepository.findAllByOrderByOrderAsc();
        var first = all.stream().findFirst();
        assertEquals(result, first.get());
    }

    @Test
    void updating_resource_types_keeps_order() throws Exception {
        var allBefore = resourceTypeRepository.findAllByOrderByOrderAsc();

        var oldOrders = allBefore.stream().map(ResourceType::getOrder).toList();
        var newOrder = oldOrders.getLast() + 1;

        // add new resource type at the end
        URI id = builder.resourceType(rt -> rt.name("video").order(newOrder)).getPublicId();

        var all = resourceTypeRepository.findAllByOrderByOrderAsc();
        assertEquals(allBefore.size() + 1, all.size());

        var resourceTypeBefore = resourceTypeRepository.getByPublicId(id);
        assertEquals("video", resourceTypeBefore.getName());
        assertEquals(newOrder, resourceTypeBefore.getOrder());

        // update name only, order should stay the same
        testUtils.updateResource("/v1/resource-types/" + id, new ResourceTypePUT() {
            {
                name = "video-updated";
            }
        });
        resourceTypeBefore = resourceTypeRepository.getByPublicId(id);
        assertEquals("video-updated", resourceTypeBefore.getName());
        assertEquals(newOrder, resourceTypeBefore.getOrder());

        all = resourceTypeRepository.findAllByOrderByOrderAsc();

        // verify order unchanged for all resource types
        for (int i = 0; i < allBefore.size(); i++) {
            assertEquals(allBefore.get(i), all.get(i));
        }
    }

    @Test
    void updating_resource_types_order_shifts_other_resource_types() throws Exception {
        var allBefore = resourceTypeRepository.findAllByOrderByOrderAsc();
        var oldOrders = allBefore.stream().map(ResourceType::getOrder).toList();
        var newOrder = oldOrders.getLast() + 1;

        // add new resource type at the end
        URI id = builder.resourceType(rt -> rt.name("video").order(newOrder)).getPublicId();

        var all = resourceTypeRepository.findAllByOrderByOrderAsc();
        assertEquals(allBefore.size() + 1, all.size());

        var resourceType = resourceTypeRepository.getByPublicId(id);
        assertEquals("video", resourceType.getName());
        assertEquals(newOrder, resourceType.getOrder());

        // update name and order, other resource types should be shifted
        testUtils.updateResource("/v1/resource-types/" + id, new ResourceTypePUT() {
            {
                name = "video-updated";
                order = 5;
            }
        });
        resourceType = resourceTypeRepository.getByPublicId(id);
        assertEquals("video-updated", resourceType.getName());
        assertEquals(5, resourceType.getOrder());

        // verify no duplicate orders
        all = resourceTypeRepository.findAllByOrderByOrderAsc();
        for (int i = 0; i < allBefore.size(); i++) {
            if (i < 5) {
                // orders before new order unchanged
                assertEquals(allBefore.get(i), all.get(i));
            } else if (i == 5) {
                // new resource type is at index 5
                assertEquals(resourceType, all.get(i));
            } else {
                // orders after new order shifted by one
                assertEquals(allBefore.get(i), all.get(i + 1));
            }
        }
    }

    @Test
    void deleting_resource_types_order_shifts_for_rest() throws Exception {
        var allBefore = resourceTypeRepository.findAllByOrderByOrderAsc();
        var oldOrders = allBefore.stream().map(ResourceType::getOrder).toList();
        var newOrder = oldOrders.getLast() + 1;

        // add new resource type at the end
        URI id = builder.resourceType(rt -> rt.name("video").order(newOrder)).getPublicId();

        var all = resourceTypeRepository.findAllByOrderByOrderAsc();
        assertEquals(allBefore.size() + 1, all.size());

        var resourceType = resourceTypeRepository.getByPublicId(id);
        assertEquals("video", resourceType.getName());
        assertEquals(newOrder, resourceType.getOrder());

        // delete and verify rest are shifted
        testUtils.deleteResource("/v1/resource-types/" + id);

        // verify resource type is deleted
        all = resourceTypeRepository.findAllByOrderByOrderAsc();
        assertTrue(
                all.stream().filter(rt -> "video".equals(rt.getName())).toList().isEmpty());

        // verify order unchanged for all resource types
        for (int i = 0; i < allBefore.size(); i++) {
            assertEquals(allBefore.get(i), all.get(i));
        }
    }
}

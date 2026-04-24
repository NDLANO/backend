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
import java.util.Optional;
import java.util.stream.Collectors;
import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.JsonGrepCode;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.commands.ResourcePostPut;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.NodeWithParents;
import no.ndla.taxonomy.service.dtos.ResourceTypeWithConnectionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourcesTest extends RestTest {
    @Autowired
    private TestSeeder testSeeder;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_resource() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .child(t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1").resource(true, r -> r.name(
                                "introduction to trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:resource:1"))));

        final var response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var resource = testUtils.getObject(NodeDTO.class, response);

        assertEquals("introduction to trigonometry", resource.getName());
        assertEquals("Optional[urn:article:1]", resource.getContentUri().toString());
        assertEquals("/subject:1/topic:1/resource:1", resource.getPath().get());

        assertTrue(resource.getMetadata().isVisible());
        assertEquals(0, resource.getMetadata().getGrepCodes().size());
    }

    @Test
    public void primary_url_is_return_when_getting_single_resource() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .resource("resource", r -> r.publicId("urn:resource:1"))));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .child("primary", NodeType.TOPIC, t -> t.publicId("urn:topic:2").resource("resource", true)));

        var response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var resource = testUtils.getObject(NodeDTO.class, response);

        assertEquals("/subject:2/topic:2/resource:1", resource.getPath().get());
    }

    @Test
    public void resource_without_subject_and_topic_has_no_url() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1"));

        var response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var resource = testUtils.getObject(NodeDTO.class, response);

        assertTrue(resource.getPath().isEmpty());
    }

    @Test
    public void can_get_all_resources() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT).isContext(true).child(t -> t.nodeType(NodeType.TOPIC)
                .resource(true, r -> r.name("The inner planets"))));
        builder.node(s -> s.nodeType(NodeType.SUBJECT).isContext(true).child(t -> t.nodeType(NodeType.TOPIC)
                .resource(true, r -> r.name("Gas giants"))));

        var response = testUtils.getResource("/v1/resources");
        final var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, s -> "The inner planets".equals(s.getName()));
        assertAnyTrue(resources, s -> "Gas giants".equals(s.getName()));
        assertAllTrue(resources, s -> isValidId(s.getId()));
        assertAllTrue(resources, r -> r.getPath().isPresent());

        assertAllTrue(resources, r -> r.getMetadata() != null);
        assertAllTrue(resources, r -> r.getMetadata().isVisible());
        assertAllTrue(resources, r -> r.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_get_resources_by_contentURI() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.resource(true, r -> {
                    r.name("The inner planets");
                    r.contentUri("urn:test:1");
                })));

        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.resource(true, r -> {
                    r.name("Gas giants");
                    r.contentUri("urn:test:2");
                })));

        {
            final var response = testUtils.getResource("/v1/resources?contentURI=urn:test:1");
            final var resources = testUtils.getObject(NodeDTO[].class, response);

            assertEquals(1, resources.length);
            assertEquals("The inner planets", resources[0].getName());
        }

        {
            final var response = testUtils.getResource("/v1/resources?contentURI=urn:test:2");
            final var resources = testUtils.getObject(NodeDTO[].class, response);

            assertEquals(1, resources.length);
            assertEquals("Gas giants", resources[0].getName());
        }
    }

    @Test
    public void can_get_resources_by_metadata() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.resource(true, r -> r.name("The inner planets")
                        .contentUri("urn:test:1")
                        .isVisible(false))));

        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.resource(true, r -> r.name("Gas giants")
                        .contentUri("urn:test:2")
                        .customField("custom", "value")
                        .isVisible(true))));

        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.resource(true, r -> r.name("Observing Uranus")
                        .contentUri("urn:test:3")
                        .customField("key", "value")
                        .isVisible(false))));

        {
            final var response = testUtils.getResource("/v1/resources?key=custom&value=value");
            final var resources = testUtils.getObject(NodeDTO[].class, response);

            assertEquals(1, resources.length);
            assertEquals("Gas giants", resources[0].getName());
        }
        {
            final var response = testUtils.getResource("/v1/resources?value=value");
            final var resources = testUtils.getObject(NodeDTO[].class, response);

            assertEquals(2, resources.length);
            assertAnyTrue(resources, s -> "Gas giants".equals(s.getName()));
            assertAnyTrue(resources, s -> "Observing Uranus".equals(s.getName()));
        }
        {
            final var response = testUtils.getResource("/v1/resources?key=key&isVisible=false");
            final var resources = testUtils.getObject(NodeDTO[].class, response);

            assertEquals(1, resources.length);
            assertEquals("Observing Uranus", resources[0].getName());
        }
    }

    @Test
    public void can_create_resource() throws Exception {
        URI id = getId(testUtils.createResource("/v1/resources", new ResourcePostPut() {
            {
                name = "testresource";
                contentUri = URI.create("urn:article:1");
            }
        }));

        var resource = nodeRepository.getByPublicId(id);
        assertEquals("testresource", resource.getName());
        assertEquals("urn:article:1", resource.getContentUri().toString());
    }

    @Test
    public void can_create_resource_with_id() throws Exception {
        final var command = new ResourcePostPut() {
            {
                id = Optional.of(URI.create("urn:resource:1"));
                name = "name";
            }
        };

        testUtils.createResource("/v1/resources", command);

        assertNotNull(nodeRepository.getByPublicId(command.getId().get()));
    }

    @Test
    public void can_clone_resource() throws Exception {
        URI publicId = builder.node(NodeType.RESOURCE, r -> r.name("Resource")
                        .resourceType(rt -> rt.name("Fagstoff"))
                        .translation("nb", tr -> tr.name("Fagstoff nb"))
                        .contentUri("urn:article:1"))
                .getPublicId();

        final var command = new ResourcePostPut() {
            {
                contentUri = URI.create("urn:article:2");
            }
        };
        URI id = getId(testUtils.createResource("/v1/resources/" + publicId + "/clone", command));
        assertNotNull(id);

        var oldRes = nodeRepository.getByPublicId(publicId);
        var newRes = nodeRepository.getByPublicId(id);
        assertNotEquals(publicId, id);
        assertEquals(oldRes.getName(), newRes.getName());
        assertEquals("urn:article:1", oldRes.getContentUri().toString());
        assertEquals("urn:article:2", newRes.getContentUri().toString());
        assertNotEquals(
                oldRes.getContentUri().toString(), newRes.getContentUri().toString());
        assertEquals(oldRes.getTranslations().size(), newRes.getTranslations().size());
        assertEquals(oldRes.getResourceTypes().size(), newRes.getResourceTypes().size());

        // contentUri can be null
        URI id2 = getId(testUtils.createResource("/v1/resources/" + publicId + "/clone", new ResourcePostPut()));
        assertNotNull(id2);
        var resWithoutContentUri = nodeRepository.findByPublicId(id2);
        assertNull(resWithoutContentUri.getContentUri());
    }

    @Test
    public void can_update_resource() throws Exception {
        URI publicId = newResource().getPublicId();

        final var command = new ResourcePostPut() {
            {
                id = Optional.of(publicId);
                name = "The inner planets";
                contentUri = URI.create("urn:article:1");
            }
        };

        testUtils.updateResource("/v1/resources/" + publicId, command);

        var resource = nodeRepository.getByPublicId(publicId);
        assertEquals(command.name, resource.getName());
        assertEquals(command.contentUri, resource.getContentUri());
    }

    @Test
    public void can_update_resource_with_new_id() throws Exception {
        URI publicId = newResource().getPublicId();
        URI randomId = URI.create("urn:resource:random");

        final var command = new ResourcePostPut() {
            {
                id = Optional.of(randomId);
                name = "The inner planets";
                contentUri = URI.create("urn:article:1");
            }
        };

        testUtils.updateResource("/v1/resources/" + publicId, command);

        var resource = nodeRepository.getByPublicId(randomId);
        assertEquals(command.name, resource.getName());
        assertEquals(command.contentUri, resource.getContentUri());
    }

    @Test
    public void can_update_resource_without_changing_metadata() throws Exception {
        URI publicId = builder.node(
                        NodeType.RESOURCE,
                        r -> r.isVisible(false).grepCode("KM123").customField("key", "value"))
                .getPublicId();

        final var command = new ResourcePostPut() {
            {
                id = Optional.of(publicId);
                name = "physics";
                contentUri = URI.create("urn:article:1");
            }
        };

        testUtils.updateResource("/v1/resources/" + publicId, command);

        var resource = nodeRepository.getByPublicId(publicId);
        assertEquals(command.name, resource.getName());
        assertEquals(command.contentUri, resource.getContentUri());

        assertFalse(resource.getMetadata().isVisible());
        assertTrue(resource.getMetadata().getGrepCodes().stream()
                .map(JsonGrepCode::getCode)
                .collect(Collectors.toSet())
                .contains("KM123"));
        assertTrue(resource.getMetadata().getCustomFields().containsValue("value"));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new ResourcePostPut() {
            {
                id = Optional.of(URI.create("urn:resource:1"));
                name = "name";
            }
        };

        testUtils.createResource("/v1/resources", command, status().isCreated());
        testUtils.createResource("/v1/resources", command, status().isConflict());
    }

    @Test
    public void can_delete_resource() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, r -> r.translation("nb", tr -> tr.name("ressurs"))
                .resourceType(rt -> rt.name("Learning path")));
        resource.getTranslation("nb");

        builder.node(t -> t.nodeType(NodeType.TOPIC).resource(resource));

        URI id = builder.node(NodeType.RESOURCE).getPublicId();
        testUtils.deleteResource("/v1/resources/" + id);
        assertNull(nodeRepository.findByPublicId(id));
    }

    @Test
    public void can_delete_resource_with_two_parent_topics() throws Exception {
        var resource = builder.node(NodeType.RESOURCE);

        builder.node(child -> child.nodeType(NodeType.TOPIC).resource(resource)).name("DELETE EDGE TO ME");
        builder.node(child -> child.nodeType(NodeType.TOPIC).resource(resource)).name("DELETE EDGE TO ME ALSO");

        final var publicId = resource.getPublicId();

        testUtils.deleteResource("/v1/resources/" + publicId);
        assertNull(nodeRepository.findByPublicId(publicId));
    }

    @Test
    public void can_get_resource_by_id() throws Exception {
        var resource = newResource();
        resource.setName("The inner planets");
        URI id = resource.getPublicId();

        var response = testUtils.getResource("/v1/resources/" + id);
        final var result = testUtils.getObject(NodeDTO.class, response);

        assertEquals(id, result.getId());
    }

    @Test
    public void get_unknown_resource_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/resources/nonexistantid", status().isNotFound());
    }

    @Test
    public void can_get_resource_types() throws Exception {
        builder.resourceType(rt -> rt.name("Subject matter")
                .publicId("urn:resourcetype:1")
                .subtype("article", st -> st.name("Article").publicId("urn:resourcetype:2"))
                .subtype("video", st -> st.name("Video").publicId("urn:resourcetype:3")));

        builder.node(
                NodeType.RESOURCE,
                r -> r.publicId("urn:resource:1").resourceType("article").resourceType("video"));

        var response = testUtils.getResource("/v1/resources/urn:resource:1/resource-types");
        final var result = testUtils.getObject(ResourceTypeWithConnectionDTO[].class, response);
        assertEquals(2, result.length);
        assertAnyTrue(
                result,
                rt -> "Article".equals(rt.getName())
                        && "urn:resourcetype:2".equals(rt.getId().toString())
                        && "urn:resourcetype:1".equals(rt.getParentId().get().toString())
                        && rt.getConnectionId().toString().contains("urn:resource-resourcetype"));
        assertAnyTrue(
                result,
                rt -> "Video".equals(rt.getName())
                        && "urn:resourcetype:3".equals(rt.getId().toString())
                        && "urn:resourcetype:1".equals(rt.getParentId().get().toString()));
    }

    @Test
    public void resources_can_have_same_name() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1").name("What is maths?"));

        final var command = new ResourcePostPut() {
            {
                id = Optional.of(URI.create("urn:resource:2"));
                name = "What is maths?";
            }
        };

        testUtils.createResource("/v1/resources", command, status().isCreated());
    }

    @Test
    public void get_resource_with_related_topics_filters_resourceTypes() throws Exception {
        final ResourceType resourceType =
                builder.resourceType(rt -> rt.name("Læringssti").translation("nb", tr -> tr.name("Læringssti")));
        final var resource = builder.node(
                NodeType.RESOURCE, r -> r.publicId("urn:resource:1").resourceType(resourceType));
        final Node topic = builder.node("primary", NodeType.TOPIC, t -> t.name("Philosophy and Mind")
                .publicId("urn:topic:1")
                .contentUri(URI.create("urn:article:6662"))
                .resource(resource, true));

        var response = testUtils.getResource("/v1/resources/" + resource.getPublicId() + "/full");
        final var result = testUtils.getObject(NodeWithParents.class, response);

        assertEquals(resource.getPublicId(), result.getId());
        assertEquals(resource.getName(), result.getName());
        assertEquals(1, result.getResourceTypes().size());
        assertEquals(
                resourceType.getName(),
                result.getResourceTypes().iterator().next().getName());
        assertEquals(1, result.getParents().size());
        final NodeChildDTO t = result.getParents().iterator().next();
        assertEquals(topic.getName(), t.getName());
        assertTrue(t.isPrimary());
        assertEquals(Optional.of(URI.create("urn:article:6662")), t.getContentUri());
    }

    @Test
    public void full_resource_has_all_paths() throws Exception {
        testSeeder.resourceInDualSubjectsTestSetup();

        var response = testUtils.getResource("/v1/resources/urn:resource:1/full");
        final var result = testUtils.getObject(NodeWithParents.class, response);
        assertNotNull(result.getPaths());
        assertEquals(2, result.getPaths().size());
        assertEquals(3, result.getBreadcrumbs().size());
    }

    @Test
    public void resource_has_all_paths() throws Exception {
        testSeeder.resourceInDualSubjectsTestSetup();

        var response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var result = testUtils.getObject(NodeWithParents.class, response);
        assertNotNull(result.getPaths());
        assertEquals(2, result.getPaths().size());
        assertEquals(3, result.getBreadcrumbs().size());
    }

    @Test
    public void can_get_resource_connection_id() throws Exception {
        Node topic = builder.node(
                t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1").resource());
        var response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        final var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(first(topic.getResourceChildren()).getPublicId(), result[0].getConnectionId());
    }

    @Test
    public void can_get_resource_connections_with_metadata() throws Exception {
        Node topic = builder.node(
                t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1").resource());
        var response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        final var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(first(topic.getResourceChildren()).getPublicId(), result[0].getConnectionId());
        assertAllTrue(result, connection -> connection.getMetadata() != null);
        assertAllTrue(result, connection -> connection.getMetadata().isVisible());
        assertAllTrue(
                result, connection -> connection.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_get_resource_connection_id_recursively() throws Exception {
        builder.node("topic", NodeType.TOPIC, t -> t.publicId("urn:topic:1343")
                .resource(r -> r.name("a").publicId("urn:resource:1"))
                .child("subtopic", NodeType.TOPIC, st -> st.publicId("urn:topic:2")
                        .resource(r -> r.name("b").publicId("urn:resource:2"))));

        var response = testUtils.getResource("/v1/topics/urn:topic:1343/resources?recursive=true");
        final var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(first(builder.node("topic").getResourceChildren()).getPublicId(), result[0].getConnectionId());
        assertEquals(first(builder.node("subtopic").getResourceChildren()).getPublicId(), result[1].getConnectionId());
    }

    @Test
    public void can_get_resources_for_a_topic_recursively() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .name("subject a")
                .child(t -> t.nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:a")
                        .name("a")
                        .resource(true, r -> r.publicId("urn:resource:1")
                                .name("resource a")
                                .contentUri("urn:article:a"))
                        .child(st -> st.nodeType(NodeType.TOPIC)
                                .publicId("urn:topic:a:1")
                                .name("aa")
                                .resource(true, r -> r.name("resource aa").contentUri("urn:article:aa"))
                                .child(st2 -> st2.nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:a:1:1")
                                        .name("aaa")
                                        .resource(true, r -> r.name("resource aaa")
                                                .contentUri("urn:article:aaa")))
                                .child(st2 -> st2.nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:a:1:2")
                                        .name("aab")
                                        .resource(true, r -> r.name("resource aab")
                                                .contentUri("urn:article:aab"))))));

        var response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true");
        final var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(
                result,
                r -> "resource a".equals(r.getName())
                        && "Optional[urn:article:a]".equals(r.getContentUri().toString()));
        assertAnyTrue(
                result,
                r -> "resource aa".equals(r.getName())
                        && "Optional[urn:article:aa]".equals(r.getContentUri().toString()));
        assertAnyTrue(
                result,
                r -> "resource aaa".equals(r.getName())
                        && "Optional[urn:article:aaa]".equals(r.getContentUri().toString()));
        assertAnyTrue(
                result,
                r -> "resource aab".equals(r.getName())
                        && "Optional[urn:article:aab]".equals(r.getContentUri().toString()));
        assertAllTrue(result, r -> !r.getPaths().isEmpty());
        assertAllTrue(result, NodeChildDTO::isPrimary);
    }

    @Test
    public void resources_by_topic_id_recursively_are_ordered_by_rank_in_parent() throws Exception {
        testSeeder.resourcesBySubjectIdTestSetup();
        var response = testUtils.getResource("/v1/topics/urn:topic:5/resources?recursive=true");
        final var result = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(6, result.length);
        assertEquals("urn:resource:3", result[0].getId().toString());
        assertEquals("urn:resource:5", result[1].getId().toString());
        assertEquals("urn:resource:4", result[2].getId().toString());
        assertEquals("urn:resource:6", result[3].getId().toString());
        assertEquals("urn:resource:7", result[4].getId().toString());
        assertEquals("urn:resource:8", result[5].getId().toString());
    }

    @Test
    public void primary_status_is_returned_on_resources() throws Exception {
        final var resource = builder.node("r1", NodeType.RESOURCE, rb -> rb.name("resource 1"));

        builder.node(tb -> {
            tb.nodeType(NodeType.TOPIC);
            tb.name("topic 1");
            tb.publicId("urn:topic:rt:1201");
            tb.resource(resource, true);
        });

        builder.node(tb -> {
            tb.nodeType(NodeType.TOPIC);
            tb.name("topic 2");
            tb.publicId("urn:topic:rt:1202");
            tb.resource(resource, false);
        });

        {
            var response = testUtils.getResource("/v1/topics/urn:topic:rt:1201/resources");
            final var result = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, result.length);
            assertTrue(result[0].isPrimary());
        }
        {
            var response = testUtils.getResource("/v1/topics/urn:topic:rt:1202/resources");
            final var result = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, result.length);
            assertFalse(result[0].isPrimary());
        }
    }

    @Test
    public void can_get_urls_for_resources_for_a_topic_recursively() throws Exception {
        builder.node(s -> s.publicId("urn:subject:1")
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:a")
                        .resource(true, r -> r.publicId("urn:resource:a"))
                        .child(st -> st.nodeType(NodeType.TOPIC)
                                .publicId("urn:topic:aa")
                                .resource(true, r -> r.publicId("urn:resource:aa"))
                                .child(st2 -> st2.nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:aaa")
                                        .resource("aaa", true, r -> r.publicId("urn:resource:aaa")))
                                .child(st2 -> st2.nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:aab")
                                        .resource(true, r -> r.publicId("urn:resource:aab"))))));

        var response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true");
        final var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(
                result, r -> "/subject:1/topic:a/resource:a".equals(r.getPath().get()));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/resource:aa"
                .equals(r.getPath().get()));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aaa/resource:aaa"
                .equals(r.getPath().get()));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aab/resource:aab"
                .equals(r.getPath().get()));
        assertAllTrue(result, NodeChildDTO::isPrimary);
    }

    @Test
    public void can_get_resources_for_a_topic_without_child_topic_resources() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT).isContext(true).child(t -> t.nodeType(NodeType.TOPIC)
                .name("a")
                .publicId("urn:topic:1")
                .child(st -> st.nodeType(NodeType.TOPIC).name("subtopic").resource(r -> r.name("subtopic resource")))
                .resource(r -> r.name("resource 1"))
                .resource(r -> r.name("resource 2"))));

        var response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        final var result = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "resource 1".equals(r.getName()));
        assertAnyTrue(result, r -> "resource 2".equals(r.getName()));
    }

    @Test
    public void resources_can_be_filtered_by_relevance() throws Exception {
        testSeeder.resourceWithRelevancesTestSetup();

        var response = testUtils.getResource("/v1/topics/urn:topic:1/resources?relevance=urn:relevance:core");
        final var resources = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(9, resources.length);

        var response2 = testUtils.getResource("/v1/topics/urn:topic:1/resources?relevance=urn:relevance:supplementary");
        final var resources2 = testUtils.getObject(NodeChildDTO[].class, response2);
        assertEquals(1, resources2.length);
    }

    @Test
    public void resources_without_filters_can_be_filtered_by_relevance() throws Exception {
        testSeeder.resourceWithRelevancesButWithoutFiltersTestSetup();

        {
            var response = testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, resources.length);
        }
        {
            var response = testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, resources.length);
        }

        {
            var response =
                    testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(0, resources.length);
        }
        {
            MockHttpServletResponse response =
                    testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, resources.length);
        }
    }

    @Test
    public void resources_without_filters_can_be_filtered_by_relevance_and_core_is_default() throws Exception {
        testSeeder.resourceWithRelevancesAndOneNullRelevanceButWithoutFiltersTestSetup();

        {
            var response = testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, resources.length);
        }
        {
            var response = testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, resources.length);
        }

        {
            var response =
                    testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(0, resources.length);
        }
        {
            var response =
                    testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, resources.length);
        }
    }

    @Test
    public void resources_are_ordered_relative_to_parent() throws Exception {
        testSeeder.resourcesBySubjectNodeIdTestSetup();

        var response = testUtils.getResource("/v1/subjects/urn:subject:1/resources");
        final var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(10, resources.length);
        assertEquals("R:9", resources[0].getName());
        assertEquals("R:1", resources[1].getName());
        assertEquals("R:2", resources[2].getName());
        assertEquals("R:10", resources[3].getName());
        assertEquals("R:3", resources[4].getName());
        assertEquals("R:5", resources[5].getName());
        assertEquals("R:4", resources[6].getName());
        assertEquals("R:6", resources[7].getName());
        assertEquals("R:7", resources[8].getName());
        assertEquals("R:8", resources[9].getName());
    }

    @Test
    public void resources_can_have_content_uri() throws Exception {
        URI id = builder.node(n -> n.nodeType(NodeType.SUBJECT)
                        .isContext(true)
                        .publicId("urn:subject:1")
                        .child(c -> c.nodeType(NodeType.TOPIC)
                                .publicId("urn:topic:1:1")
                                .resource(r -> r.publicId("urn:resource:1").contentUri("urn:article:1"))))
                .getPublicId();

        var response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        final var resources = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals("Optional[urn:article:1]", resources[0].getContentUri().toString());
        // assertEquals("/subject:1/topic:1:1/resource:1", resources[0].getPath());
    }

    @Test
    public void can_get_resources_for_a_subject_and_its_topics_recursively_with_metadata() throws Exception {
        URI id = builder.node(NodeType.SUBJECT, n -> n.isContext(true)
                        .publicId("urn:subject:1")
                        .name("Subject")
                        .child("topic a", NodeType.TOPIC, t -> t.name("topic a")
                                .publicId("urn:topic:1")
                                .resource(r -> r.name("resource a").resourceType(rt -> rt.name("assignment"))))
                        .child("topic b", NodeType.TOPIC, t -> t.name("topic b")
                                .publicId("urn:topic:2")
                                .resource(r -> r.name("resource b").resourceType(rt -> rt.name("lecture")))
                                .child("subtopic", NodeType.TOPIC, s -> s.name("subtopic")
                                        .publicId("urn:topic:3")
                                        .resource(r -> r.name("sub resource")))))
                .getPublicId();

        var response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        final var resources = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(3, resources.length);

        assertAnyTrue(resources, r -> r.getConnectionId()
                .equals(first(builder.node("topic a").getResourceChildren()).getPublicId()));
        assertAnyTrue(resources, r -> r.getConnectionId()
                .equals(first(builder.node("topic b").getResourceChildren()).getPublicId()));
        assertAnyTrue(resources, r -> r.getConnectionId()
                .equals(first(builder.node("subtopic").getResourceChildren()).getPublicId()));

        assertAllTrue(resources, r -> r.getMetadata() != null);
        assertAllTrue(resources, r -> r.getMetadata().isVisible());
        assertAllTrue(resources, r -> r.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_get_urls_for_all_resources() throws Exception {
        builder.node(n -> n.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .child(c -> c.nodeType(NodeType.TOPIC).publicId("urn:topic:1").resource(true, r -> r.name("resource 1")
                        .publicId("urn:resource:1")
                        .resourceType(rt -> rt.name("lecture"))))
                .child(c -> c.nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:2")
                        .resource(true, r -> r.name("resource 2")
                                .publicId("urn:resource:2")
                                .resourceType(rt -> rt.name("lecture")))
                        .child(c1 -> c1.nodeType(NodeType.TOPIC)
                                .publicId("urn:topic:3")
                                .resource(true, r -> r.name("resouce 3")
                                        .publicId("urn:resource:3")
                                        .resourceType(rt -> rt.name("lecture"))))));

        var response = testUtils.getResource("/v1/subjects/urn:subject:1/resources");
        final var resources = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(3, resources.length);
        assertAnyTrue(resources, r -> "/subject:1/topic:1/resource:1"
                .equals(r.getPath().get()));
        assertAnyTrue(resources, r -> "/subject:1/topic:2/resource:2"
                .equals(r.getPath().get()));
        assertAnyTrue(resources, r -> "/subject:1/topic:2/topic:3/resource:3"
                .equals(r.getPath().get()));
    }
}

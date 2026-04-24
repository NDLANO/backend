/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.LanguageFieldDTO;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyContextDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class QueryTest extends RestTest {

    @BeforeEach
    public void add_core_relevance() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_resource_by_contentURI() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals(1, resources[0].getResourceTypes().size());
    }

    @Test
    public void no_resources_matching_contentURI() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(0, resources.length);
    }

    @Test
    public void can_get_all_resources_matching_contentURI() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")));

        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:2").contentUri("urn:article:3"));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertAnyTrue(resources, r -> "urn:resource:1".equals(r.getId().toString()));
    }

    @Test
    public void can_get_all_resource_types_for_a_resource() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material").order(1))
                .resourceType(rt -> rt.name("Learning path").order(2)));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals(2, resources[0].getResourceTypes().size());
    }

    @Test
    public void can_get_translated_name_for_resource() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .name("Resource")
                .translation("nb", tr -> tr.name("ressurs"))
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")));

        MockHttpServletResponse response =
                testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345&language=nb");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals(1, resources[0].getResourceTypes().size());
        assertEquals("ressurs", resources[0].getName());
    }

    @Test
    public void can_get_a_topic_matching_contentURI() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")));

        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:2").contentUri("urn:article:345"));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/topics?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.getId().toString()));
    }

    @Test
    public void can_get_all_topics_matching_contentURI() throws Exception {
        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:2").contentUri("urn:article:345"));

        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:3").contentUri("urn:article:345"));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/topics?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.getId().toString()));
        assertAnyTrue(resources, r -> "urn:topic:3".equals(r.getId().toString()));
    }

    @Test
    public void no_topics_matching_contentURI() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/queries/topics?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(0, resources.length);
    }

    @Test
    public void can_get_translated_name_for_topic() throws Exception {
        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:2")
                .name("topic")
                .translation("nb", tr -> tr.name("Emne"))
                .contentUri("urn:article:345"));

        MockHttpServletResponse response =
                testUtils.getResource("/v1/queries/topics?contentURI=urn:article:345&language=nb");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals("Emne", resources[0].getName());
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.getId().toString()));
    }

    @Test
    public void can_get_searchable_context_with_visible_filtering() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:1")
                .translation("nb", t -> t.name("Ressurs")));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .name("subject")
                .translation("nb", tr -> tr.name("Fag"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .name("topic")
                        .translation("nb", tr -> tr.name("Emne"))
                        .child(resource)));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .name("subject 2")
                .translation("nb", tr -> tr.name("Fag 2"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:2")
                        .name("topic 2")
                        .translation("nb", tr -> tr.name("Emne 2"))
                        .child(resource)));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .isVisible(false)
                .publicId("urn:subject:3")
                .name("subject 3")
                .translation("nb", tr -> tr.name("Fag 3"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:3")
                        .name("topic 3")
                        .translation("nb", tr -> tr.name("Emne 3"))
                        .child(resource)));

        var response = testUtils.getResource("/v1/queries/urn:article:1?filterVisibles=true");
        var result = testUtils.getObject(TaxonomyContextDTO[].class, response);

        assertEquals(2, result.length);

        var firstResult = result[0];
        assertEquals(URI.create("urn:resource:1"), firstResult.publicId());
        assertEquals(
                List.of(URI.create("urn:subject:1"), URI.create("urn:topic:1")),
                firstResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:subject:1"), firstResult.rootId());
        assertEquals("/subject:1/topic:1/resource:1", firstResult.path());
        assertEquals(URI.create("urn:relevance:core"), firstResult.relevanceId());
        var breadcrumbs = new LanguageFieldDTO<List<String>>();
        breadcrumbs.put("nb", List.of("Fag", "Emne"));
        assertEquals(breadcrumbs, firstResult.breadcrumbs());

        var secondResult = result[1];
        assertEquals(URI.create("urn:resource:1"), secondResult.publicId());
        assertEquals(
                List.of(URI.create("urn:subject:2"), URI.create("urn:topic:2")),
                secondResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:subject:2"), secondResult.rootId());
        assertEquals("/subject:2/topic:2/resource:1", secondResult.path());
        assertEquals(URI.create("urn:relevance:core"), secondResult.relevanceId());
        var breadcrumbs2 = new LanguageFieldDTO<List<String>>();
        breadcrumbs2.put("nb", List.of("Fag 2", "Emne 2"));
        assertEquals(breadcrumbs2, secondResult.breadcrumbs());
    }

    @Test
    public void can_get_searchable_context_with_no_visible_filtering() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:1")
                .translation("nb", t -> t.name("Ressurs")));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .name("subject")
                .translation("nb", tr -> tr.name("Fag"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .name("topic")
                        .translation("nb", tr -> tr.name("Emne"))
                        .child(resource)));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .name("subject 2")
                .translation("nb", tr -> tr.name("Fag 2"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:2")
                        .name("topic 2")
                        .translation("nb", tr -> tr.name("Emne 2"))
                        .child(resource)));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .isVisible(false)
                .publicId("urn:subject:3")
                .name("subject 3")
                .translation("nb", tr -> tr.name("Fag 3"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:3")
                        .name("topic 3")
                        .translation("nb", tr -> tr.name("Emne 3"))
                        .child(resource)));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/urn:article:1?filterVisibles=false");
        var result = testUtils.getObject(TaxonomyContextDTO[].class, response);

        assertEquals(3, result.length);
        var firstResult = result[0];
        assertEquals(URI.create("urn:resource:1"), firstResult.publicId());
        assertEquals(
                List.of(URI.create("urn:subject:1"), URI.create("urn:topic:1")),
                firstResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:subject:1"), firstResult.rootId());
        assertEquals("/subject:1/topic:1/resource:1", firstResult.path());
        assertEquals(URI.create("urn:relevance:core"), firstResult.relevanceId());
        var breadcrumbs = new LanguageFieldDTO<List<String>>();
        breadcrumbs.put("nb", List.of("Fag", "Emne"));
        assertEquals(breadcrumbs, firstResult.breadcrumbs());

        var secondResult = result[1];
        assertEquals(URI.create("urn:resource:1"), secondResult.publicId());
        assertEquals(
                List.of(URI.create("urn:subject:2"), URI.create("urn:topic:2")),
                secondResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:subject:2"), secondResult.rootId());
        assertEquals("/subject:2/topic:2/resource:1", secondResult.path());
        assertEquals(URI.create("urn:relevance:core"), secondResult.relevanceId());
        var breadcrumbs2 = new LanguageFieldDTO<List<String>>();
        breadcrumbs2.put("nb", List.of("Fag 2", "Emne 2"));
        assertEquals(breadcrumbs2, secondResult.breadcrumbs());

        var thirdResult = result[2];
        assertEquals(URI.create("urn:resource:1"), thirdResult.publicId());
        assertEquals(
                List.of(URI.create("urn:subject:3"), URI.create("urn:topic:3")),
                thirdResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:subject:3"), thirdResult.rootId());
        assertEquals("/subject:3/topic:3/resource:1", thirdResult.path());
        assertEquals(URI.create("urn:relevance:core"), thirdResult.relevanceId());
        var breadcrumbs3 = new LanguageFieldDTO<List<String>>();
        breadcrumbs3.put("nb", List.of("Fag 3", "Emne 3"));
        assertEquals(breadcrumbs3, thirdResult.breadcrumbs());
    }

    @Test
    public void that_parent_topic_ids_are_derived_correctly() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:1")
                .translation("nb", t -> t.name("Ressurs")));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .name("subject")
                .translation("nb", tr -> tr.name("Fag"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .name("topic")
                        .translation("nb", tr -> tr.name("Emne"))
                        .child(resource)));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .name("subject 2")
                .translation("nb", tr -> tr.name("Fag 2"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:2")
                        .name("topic 2")
                        .translation("nb", tr -> tr.name("Emne 2"))
                        .child(resource)));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/urn:article:1?filterVisibles=false");
        var result = testUtils.getObject(TaxonomyContextDTO[].class, response);

        assertEquals(2, result.length);
        assertEquals(
                List.of(URI.create("urn:subject:1"), URI.create("urn:topic:1")),
                result[0].parentIds().stream().sorted().toList());
        assertEquals(
                List.of(URI.create("urn:subject:2"), URI.create("urn:topic:2")),
                result[1].parentIds().stream().sorted().toList());
    }

    @Test
    public void that_topics_can_be_contexts_too() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .contentUri("urn:article:1")
                .translation("nb", t -> t.name("Ressurs")));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .name("subject")
                .translation("nb", tr -> tr.name("Fag"))
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .name("topic")
                        .translation("nb", tr -> tr.name("Emne"))
                        .child(resource)));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .name("subject 2")
                .translation("nb", tr -> tr.name("Fag 2"))
                .child(NodeType.TOPIC, t -> t.isContext(true)
                        .publicId("urn:topic:2")
                        .name("topic 2")
                        .translation("nb", tr -> tr.name("Emne 2"))
                        .child(resource)));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/urn:article:1?filterVisibles=false");
        var result = testUtils.getObject(TaxonomyContextDTO[].class, response);

        assertEquals(3, result.length);
        var firstResult = result[0];
        assertEquals(URI.create("urn:resource:1"), firstResult.publicId());
        assertEquals(
                List.of(URI.create("urn:subject:1"), URI.create("urn:topic:1")),
                firstResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:subject:1"), firstResult.rootId());
        assertEquals("/subject:1/topic:1/resource:1", firstResult.path());
        assertEquals(URI.create("urn:relevance:core"), firstResult.relevanceId());
        var breadcrumbs = new LanguageFieldDTO<List<String>>();
        breadcrumbs.put("nb", List.of("Fag", "Emne"));
        assertEquals(breadcrumbs, firstResult.breadcrumbs());

        var secondResult = result[1];
        assertEquals(URI.create("urn:resource:1"), secondResult.publicId());
        assertEquals(
                List.of(URI.create("urn:subject:2"), URI.create("urn:topic:2")),
                secondResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:subject:2"), secondResult.rootId());
        assertEquals("/subject:2/topic:2/resource:1", secondResult.path());
        assertEquals(URI.create("urn:relevance:core"), secondResult.relevanceId());
        var breadcrumbs2 = new LanguageFieldDTO<List<String>>();
        breadcrumbs2.put("nb", List.of("Fag 2", "Emne 2"));
        assertEquals(breadcrumbs2, secondResult.breadcrumbs());

        var thirdResult = result[2];
        assertEquals(URI.create("urn:resource:1"), thirdResult.publicId());
        assertEquals(
                List.of(URI.create("urn:topic:2")),
                thirdResult.parentIds().stream().sorted().toList());
        assertEquals(URI.create("urn:topic:2"), thirdResult.rootId());
        assertEquals("/topic:2/resource:1", thirdResult.path());
        assertEquals(URI.create("urn:relevance:core"), thirdResult.relevanceId());
        var breadcrumbs3 = new LanguageFieldDTO<List<String>>();
        breadcrumbs3.put("nb", List.of("Emne 2"));
        assertEquals(breadcrumbs3, thirdResult.breadcrumbs());
    }

    @Test
    public void that_contexts_can_be_found_by_path() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").name("Subject").child(NodeType.TOPIC, t -> t.name(
                                "Topic")
                        .publicId("urn:topic:1")
                        .resource(r -> r.name("One fine resource")
                                .contentUri("urn:article:1")
                                .publicId("urn:resource:1"))));
        Node resource = nodeRepository.getByPublicId(URI.create("urn:resource:1"));
        {
            var response = testUtils.getResource("/v1/queries/path?path="
                    + String.format(
                            "/one-fine-resource/r/%s",
                            resource.getContexts().stream().findFirst().get().contextId()));
            var result = testUtils.getObject(TaxonomyContextDTO[].class, response);

            assertEquals(1, result.length);
            assertEquals(List.of("Subject", "Topic"), result[0].breadcrumbs().get("nb"));
        }
        {
            // Old format used for programmes
            var response = testUtils.getResource("/v1/queries/path?path="
                    + String.format(
                            "/one-fine-resource__%s",
                            resource.getContexts().stream().findFirst().get().contextId()));
            var result = testUtils.getObject(TaxonomyContextDTO[].class, response);

            assertEquals(1, result.length);
            assertEquals(List.of("Subject", "Topic"), result[0].breadcrumbs().get("nb"));
        }
    }
}

/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.*;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.rest.v1.dtos.ContextDTO;
import no.ndla.taxonomy.rest.v1.dtos.ContextPOST;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

public class ContextsTest extends RestTest {
    @Autowired
    private ContextUpdaterService contextUpdaterService;

    @BeforeEach
    void cleanDatabase() {
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    public void all_subjects_are_contexts() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .publicId("urn:subject:1")
                .isContext(true)
                .name("Subject 1"));

        var response = testUtils.getResource("/v1/contexts");
        var contexts = testUtils.getObject(ContextDTO[].class, response);

        assertEquals(1, contexts.length);
        assertEquals("urn:subject:1", contexts[0].id.toString());
        assertEquals("/subject:1", contexts[0].path);
        assertEquals("Subject 1", contexts[0].name);
    }

    @Test
    public void topics_can_be_contexts() throws Exception {
        builder.node(t -> t.nodeType(NodeType.TOPIC)
                .publicId("urn:topic:1")
                .name("Topic 1")
                .isContext(true));

        var response = testUtils.getResource("/v1/contexts");
        var contexts = testUtils.getObject(ContextDTO[].class, response);

        assertEquals(1, contexts.length);
        assertEquals("urn:topic:1", contexts[0].id.toString());
        assertEquals("/topic:1", contexts[0].path);
        assertEquals("Topic 1", contexts[0].name);
    }

    @Test
    public void can_add_topic_as_context() throws Exception {
        Node topic = builder.node(t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:ct:2"));

        testUtils.createResource("/v1/contexts", new ContextPOST() {
            {
                id = topic.getPublicId();
            }
        });

        assertTrue(topic.isContext());
    }

    @Test
    public void can_remove_topic_as_context() throws Exception {
        Node topic = builder.node(
                t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1").isContext(true));

        testUtils.deleteResource("/v1/contexts/urn:topic:1");
        assertFalse(topic.isContext());
    }

    @Test
    public void can_get_translated_contexts() throws Exception {
        nodeRepository.deleteAllAndFlush();

        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .name("Subject 1")
                .translation("Fag 1", "nb"));

        builder.node(t -> t.nodeType(NodeType.TOPIC)
                .publicId("urn:topic:1")
                .name("Topic 1")
                .translation("Emne 1", "nb")
                .isContext(true));

        var response = testUtils.getResource("/v1/contexts?language=nb");
        var contexts = testUtils.getObject(ContextDTO[].class, response);

        assertEquals(2, contexts.length);

        assertAnyTrue(contexts, c -> "Emne 1".equals(c.name));
        assertAnyTrue(contexts, c -> "Fag 1".equals(c.name));
    }

    // TODO Set is not ordered
    @Test
    public void root_context_is_more_important_than_primary_parent() throws Exception {
        Node topic = builder.node(t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1"));

        builder.node(s -> s.nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .child(topic));

        topic.setContext(true);
        nodeRepository.saveAndFlush(topic);

        contextUpdaterService.updateContexts(topic);

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        final var topicIndexDocument = testUtils.getObject(NodeDTO.class, response);
        assertAnyTrue(topicIndexDocument.getPaths(), p -> "/topic:1".equals(p));
    }
}

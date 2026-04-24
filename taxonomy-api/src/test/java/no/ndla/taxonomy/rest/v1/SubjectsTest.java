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
import no.ndla.taxonomy.rest.v1.commands.SubjectPostPut;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SubjectsTest extends RestTest {
    @Autowired
    private TestSeeder testSeeder;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_subject() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("english")
                .contentUri("urn:article:1")
                .publicId("urn:subject:1"));

        var response = testUtils.getResource("/v1/subjects/urn:subject:1");
        var subject = testUtils.getObject(NodeDTO.class, response);

        assertEquals("english", subject.getName());
        assertEquals("Optional[urn:article:1]", subject.getContentUri().toString());
        assertEquals("/subject:1", subject.getPath().get());

        assertNotNull(subject.getMetadata());
        assertTrue(subject.getMetadata().isVisible());
        assertTrue(subject.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("english"));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("mathematics"));

        var response = testUtils.getResource("/v1/subjects");
        var subjects = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.getName()));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.getName()));
        assertAllTrue(subjects, s -> isValidId(s.getId()));
        assertAllTrue(subjects, s -> s.getPath().isPresent());

        assertAllTrue(subjects, s -> s.getMetadata() != null);

        assertAllTrue(subjects, s -> s.getMetadata().isVisible());
        assertAllTrue(subjects, s -> s.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_create_subject() throws Exception {
        final var createSubjectCommand = new SubjectPostPut() {
            {
                name = "testsubject";
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        var response = testUtils.createResource("/v1/subjects", createSubjectCommand);
        URI id = getId(response);

        Node subject = nodeRepository.getByPublicId(id);
        assertEquals(createSubjectCommand.name, subject.getName());
        assertEquals(createSubjectCommand.contentUri.get(), subject.getContentUri());
        assertTrue(subject.isContext()); // all subjects are contexts
    }

    @Test
    public void can_create_subject_with_id() throws Exception {
        final var command = new SubjectPostPut() {
            {
                id = Optional.of(URI.create("urn:subject:1"));
                name = "name";
            }
        };

        var response = testUtils.createResource("/v1/subjects", command);
        assertEquals("/v1/nodes/urn:subject:1", response.getHeader("Location"));

        assertNotNull(nodeRepository.getByPublicId(command.getId().get()));
    }

    @Test
    public void can_update_subject() throws Exception {
        URI publicId = builder.node(NodeType.SUBJECT).getPublicId();

        final var command = new SubjectPostPut() {
            {
                id = Optional.of(publicId);
                name = "physics";
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        testUtils.updateResource("/v1/subjects/" + publicId, command);

        Node subject = nodeRepository.getByPublicId(publicId);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri.get(), subject.getContentUri());
    }

    @Test
    public void can_update_subject_with_new_id() throws Exception {
        URI publicId = builder.node(NodeType.SUBJECT).getPublicId();
        URI randomId = URI.create("urn:subject:random");

        final var command = new SubjectPostPut() {
            {
                id = Optional.of(randomId);
                name = "random";
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        testUtils.updateResource("/v1/subjects/" + publicId, command);

        Node subject = nodeRepository.getByPublicId(randomId);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri.get(), subject.getContentUri());
    }

    @Test
    public void can_update_subject_without_changing_metadata() throws Exception {
        URI publicId = builder.node(
                        NodeType.SUBJECT,
                        s -> s.isVisible(false).grepCode("KM123").customField("key", "value"))
                .getPublicId();

        final var command = new SubjectPostPut() {
            {
                id = Optional.of(publicId);
                name = "physics";
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        testUtils.updateResource("/v1/subjects/" + publicId, command);

        Node subject = nodeRepository.getByPublicId(publicId);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri.get(), subject.getContentUri());
        assertFalse(subject.getMetadata().isVisible());
        assertTrue(subject.getMetadata().getGrepCodes().stream()
                .map(JsonGrepCode::getCode)
                .collect(Collectors.toSet())
                .contains("KM123"));
        assertTrue(subject.getCustomFields().containsValue("value"));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new SubjectPostPut() {
            {
                id = Optional.of(URI.create("urn:subject:1"));
                name = "name";
            }
        };

        testUtils.createResource("/v1/subjects", command, status().isCreated());
        testUtils.createResource("/v1/subjects", command, status().isConflict());
    }

    @Test
    public void can_delete_subject() throws Exception {
        URI id = builder.node(NodeType.SUBJECT, s -> s.child(NodeType.TOPIC, t -> t.publicId("urn:topic:1"))
                        .translation("nb", tr -> tr.name("fag")))
                .getPublicId();

        testUtils.deleteResource("/v1/subjects/" + id);
        assertNull(nodeRepository.findByPublicId(id));
    }

    @Test
    public void can_get_topics() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("physics")
                .child(NodeType.TOPIC, t -> t.name("statics").contentUri("urn:article:1"))
                .child(NodeType.TOPIC, t -> t.name("electricity").contentUri("urn:article:2"))
                .child(NodeType.TOPIC, t -> t.name("optics").contentUri("urn:article:3")));

        var response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics");
        var topics = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(
                topics,
                t -> "statics".equals(t.getName())
                        && "Optional[urn:article:1]".equals(t.getContentUri().toString()));
        assertAnyTrue(
                topics,
                t -> "electricity".equals(t.getName())
                        && "Optional[urn:article:2]".equals(t.getContentUri().toString()));
        assertAnyTrue(
                topics,
                t -> "optics".equals(t.getName())
                        && "Optional[urn:article:3]".equals(t.getContentUri().toString()));
        assertAnyTrue(topics, NodeChildDTO::isPrimary);
        assertAllTrue(topics, t -> isValidId(t.getId()));
        assertAllTrue(topics, t -> isValidId(t.getConnectionId()));
        assertAllTrue(topics, t -> t.getPath().isPresent());
        assertAllTrue(topics, t -> t.getParentId().equals(subject.getPublicId()));

        assertAllTrue(topics, t -> t.getMetadata() != null);

        assertAllTrue(topics, t -> t.getMetadata().isVisible());
        assertAllTrue(topics, t -> t.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_get_topics_recursively() throws Exception {
        URI subjectId = builder.node("subject", NodeType.SUBJECT, s -> s.isContext(true)
                        .name("subject")
                        .publicId("urn:subject:1")
                        .child("parent", NodeType.TOPIC, parent -> parent.name("parent topic")
                                .publicId("urn:topic:a")
                                .child("child", NodeType.TOPIC, child -> child.name("child topic")
                                        .publicId("urn:topic:aa")
                                        .child("grandchild", NodeType.TOPIC, grandchild -> grandchild
                                                .name("grandchild topic")
                                                .publicId("urn:topic:aaa")))))
                .getPublicId();

        var response = testUtils.getResource("/v1/subjects/" + subjectId + "/topics?recursive=true");
        var topics = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(3, topics.length);
        assertEquals("parent topic", topics[0].getName());
        assertEquals("/subject:1/topic:a", topics[0].getPath().get());
        assertEquals("child topic", topics[1].getName());
        assertEquals("/subject:1/topic:a/topic:aa", topics[1].getPath().get());
        assertEquals("grandchild topic", topics[2].getName());
        assertEquals(
                "/subject:1/topic:a/topic:aa/topic:aaa", topics[2].getPath().get());

        assertAllTrue(topics, t -> t.getMetadata() != null);

        assertAllTrue(topics, t -> t.getMetadata().isVisible());
        assertAllTrue(topics, t -> t.getMetadata().getGrepCodes().isEmpty());

        Node subject = builder.node("subject");
        assertEquals(first(subject.getChildConnections()).getPublicId(), topics[0].getConnectionId());

        Node parent = builder.node("parent");
        assertEquals(first(parent.getChildConnections()).getPublicId(), topics[1].getConnectionId());

        Node child = builder.node("child");
        assertEquals(first(child.getChildConnections()).getPublicId(), topics[2].getConnectionId());
    }

    @Test
    public void recursive_topics_are_ordered_by_rank_relative_to_parent() throws Exception {
        testSeeder.recursiveNodesBySubjectNodeIdTestSetup();

        var response = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true");
        var topics = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(8, topics.length);
        assertEquals("urn:topic:1", topics[0].getId().toString());
        assertEquals("urn:topic:2", topics[1].getId().toString());
        assertEquals("urn:topic:3", topics[2].getId().toString());
        assertEquals("urn:topic:4", topics[3].getId().toString());
        assertEquals("urn:topic:5", topics[4].getId().toString());
        assertEquals("urn:topic:6", topics[5].getId().toString());
        assertEquals("urn:topic:7", topics[6].getId().toString());
        assertEquals("urn:topic:8", topics[7].getId().toString());
    }

    @Test
    public void recursive_topics_with_relevance_are_ordered_relative_to_parent() throws Exception {
        testSeeder.recursiveNodesBySubjectNodeIdAndRelevanceTestSetup();
        // test core relevance
        var response =
                testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true&relevance=urn:relevance:core");
        var topics = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(5, topics.length);

        // test supplementary relevance
        var response2 = testUtils.getResource(
                "/v1/subjects/urn:subject:1/topics?recursive=true&relevance=urn:relevance:supplementary");
        var topics2 = testUtils.getObject(NodeChildDTO[].class, response2);
        assertEquals(3, topics2.length);
    }
}

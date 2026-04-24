/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class DomainEntityHelperServiceImplTest extends AbstractIntegrationTest {
    private Node subject1;
    private Node subject2;
    private Node topic1;
    private Node topic2;
    private Node node1;
    private Node node2;
    private Node resource1;
    private Node resource2;

    private DomainEntityHelperServiceImpl service;

    @BeforeEach
    void setUp(@Autowired NodeRepository nodeRepository, @Autowired NodeConnectionRepository nodeConnectionRepository) {
        service = new DomainEntityHelperServiceImpl(nodeRepository, nodeConnectionRepository);

        topic1 = new Node(NodeType.TOPIC);
        topic1.setPublicId(URI.create("urn:topic:test:1"));
        topic1 = nodeRepository.save(topic1);

        topic2 = new Node(NodeType.TOPIC);
        topic2.setPublicId(URI.create("urn:topic:test:2"));
        topic2 = nodeRepository.save(topic2);

        subject1 = new Node(NodeType.SUBJECT);
        subject1.setPublicId(URI.create("urn:subject:test:1"));
        subject1 = nodeRepository.save(subject1);

        subject2 = new Node(NodeType.SUBJECT);
        subject2.setPublicId(URI.create("urn:subject:test:2"));
        subject2 = nodeRepository.save(subject2);

        node1 = new Node(NodeType.NODE);
        node1.setPublicId(URI.create("urn:node:test:1"));
        node1 = nodeRepository.save(node1);

        node2 = new Node(NodeType.NODE);
        node2.setPublicId(URI.create("urn:node:test:2"));
        node2 = nodeRepository.save(node2);

        resource1 = new Node(NodeType.RESOURCE);
        resource1.setPublicId(URI.create("urn:resource:test:1"));
        resource1 = nodeRepository.save(resource1);

        resource2 = new Node(NodeType.RESOURCE);
        resource2.setPublicId(URI.create("urn:resource:test:2"));
        resource2 = nodeRepository.save(resource2);
    }

    @Test
    void getSubjectByPublicId() {
        assertSame(subject1, service.getNodeByPublicId(URI.create("urn:subject:test:1")));
        assertSame(subject2, service.getNodeByPublicId(URI.create("urn:subject:test:2")));

        assertThrows(NotFoundServiceException.class, () -> service.getNodeByPublicId(URI.create("urn:topic:test:3")));
    }

    @Test
    void getTopicByPublicId() {
        assertSame(topic1, service.getNodeByPublicId(URI.create("urn:topic:test:1")));
        assertSame(topic2, service.getNodeByPublicId(URI.create("urn:topic:test:2")));

        assertThrows(NotFoundServiceException.class, () -> service.getNodeByPublicId(URI.create("urn:topic:test:3")));
    }

    @Test
    void getNodeByPublicId() {
        assertSame(node1, service.getNodeByPublicId(URI.create("urn:node:test:1")));
        assertSame(node2, service.getNodeByPublicId(URI.create("urn:node:test:2")));

        assertThrows(NotFoundServiceException.class, () -> service.getNodeByPublicId(URI.create("urn:node:test:3")));
    }

    @Test
    void getEntityByPublicId() {
        assertSame(subject1, service.getEntityByPublicId(URI.create("urn:subject:test:1")));
        assertSame(subject2, service.getEntityByPublicId(URI.create("urn:subject:test:2")));
        assertSame(topic1, service.getEntityByPublicId(URI.create("urn:topic:test:1")));
        assertSame(topic2, service.getEntityByPublicId(URI.create("urn:topic:test:2")));
        assertSame(node1, service.getEntityByPublicId(URI.create("urn:node:test:1")));
        assertSame(node2, service.getEntityByPublicId(URI.create("urn:node:test:2")));
        assertSame(resource1, service.getEntityByPublicId(URI.create("urn:resource:test:1")));
        assertSame(resource2, service.getEntityByPublicId(URI.create("urn:resource:test:2")));

        assertNull(service.getEntityByPublicId(URI.create("urn:topic:test:3")));
        assertNull(service.getEntityByPublicId(URI.create("urn:topic:test:3")));
        assertNull(service.getEntityByPublicId(URI.create("urn:node:test:3")));
        assertNull(service.getEntityByPublicId(URI.create("urn:resource:test:3")));
    }
}

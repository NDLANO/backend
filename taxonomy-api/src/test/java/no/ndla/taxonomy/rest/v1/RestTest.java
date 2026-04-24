/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import jakarta.persistence.EntityManager;
import no.ndla.taxonomy.TestUtils;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.AbstractIntegrationTest;
import no.ndla.taxonomy.service.ContextUpdaterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("junit")
@Transactional
public class RestTest extends AbstractIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    VersionRepository versionRepository;

    @Autowired
    ResourceResourceTypeRepository resourceResourceTypeRepository;

    @Autowired
    ResourceTypeRepository resourceTypeRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    NodeConnectionRepository nodeConnectionRepository;

    @Autowired
    protected TestUtils testUtils;

    @Autowired
    protected ContextUpdaterService contextUpdaterService;

    protected Builder builder;

    @BeforeEach
    public void restTestSetUp() {
        builder = new Builder(entityManager, contextUpdaterService);
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    <T> T save(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    Node newSubject() {
        Node node = new Node(NodeType.SUBJECT);
        node.setContext(true);
        return save(node);
    }

    Node newTopic() {
        Node node = new Node(NodeType.TOPIC);
        return save(node);
    }

    Node newResource() {
        var node = new Node(NodeType.RESOURCE);
        return save(node);
    }

    ResourceType newResourceType() {
        return save(new ResourceType());
    }
}

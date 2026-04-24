/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy;

import java.net.URI;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.ContextUpdaterService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class replaces some SQL files that was used to seed the database for various tests. The SQL statements has been
 * rewritten as JPA so any in-application triggers can run.
 *
 * <p>
 * Method names refers to the old SQL file name
 */
@Transactional
@Component
public class TestSeeder {
    private final ResourceTypeRepository resourceTypeRepository;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final ContextUpdaterService cachedUrlUpdaterService;

    public TestSeeder(
            ResourceTypeRepository resourceTypeRepository,
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            ContextUpdaterService cachedUrlUpdaterService) {
        this.resourceTypeRepository = resourceTypeRepository;
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    private Node createResource(String publicId, String name, String contentUri) {
        final var resource = new Node(NodeType.RESOURCE);

        if (publicId != null) {
            resource.setPublicId(URI.create(publicId));
        }

        if (name != null) {
            resource.setName(name);
        }

        if (contentUri != null) {
            resource.setContentUri(URI.create(contentUri));
        }

        nodeRepository.saveAndFlush(resource);
        cachedUrlUpdaterService.updateContexts(resource);

        return resource;
    }

    private ResourceType createResourceType(ResourceType parent, String publicId, String name) {
        final var resourceType = new ResourceType();

        if (parent != null) {
            resourceType.setParent(parent);
        }

        if (publicId != null) {
            resourceType.setPublicId(URI.create(publicId));
        }

        if (name != null) {
            resourceType.setName(name);
        }

        return resourceTypeRepository.save(resourceType);
    }

    private void createResourceResourceType(String publicId, Node resource, ResourceType resourceType) {
        final var resourceResourceType = ResourceResourceType.create(resource, resourceType);

        if (publicId != null) {
            resourceResourceType.setPublicId(URI.create(publicId));
        }
    }

    private Node createNode(NodeType nodeType, String publicId, String name, String contentUri, Boolean context) {
        final var node = new Node(nodeType);

        if (publicId != null) {
            node.setPublicId(URI.create(publicId));
        }
        if (name != null) {
            node.setName(name);
        }
        if (contentUri != null) {
            node.setContentUri(URI.create(contentUri));
        }
        if (context != null) {
            node.setContext(context);
        }

        nodeRepository.saveAndFlush(node);

        cachedUrlUpdaterService.updateContexts(node);

        return node;
    }

    private void createNodeConnection(String publicId, Node parent, Node child, Integer rank, Relevance relevance) {
        final var nodeConnection = NodeConnection.create(parent, child, relevance);

        if (publicId != null) {
            nodeConnection.setPublicId(URI.create(publicId));
        }

        if (rank != null) {
            nodeConnection.setRank(rank);
        }

        nodeConnectionRepository.saveAndFlush(nodeConnection);

        nodeConnection.getParent().ifPresent(cachedUrlUpdaterService::updateContexts);
    }

    private void createNodeResource(
            String publicId, Node node, Node resource, Boolean isPrimary, Integer rank, Relevance relevance) {
        final var nodeResource = NodeConnection.create(node, resource, relevance);

        if (publicId != null) {
            nodeResource.setPublicId(URI.create(publicId));
        }

        if (isPrimary != null) {
            nodeResource.setPrimary(isPrimary);
        }

        if (rank != null) {
            nodeResource.setRank(rank);
        }

        nodeConnectionRepository.saveAndFlush(nodeResource);

        nodeResource.getParent().ifPresent(cachedUrlUpdaterService::updateContexts);
    }

    private void clearAll() {
        resourceTypeRepository.deleteAllAndFlush();
        nodeRepository.deleteAllAndFlush();
    }

    public void recursiveNodesBySubjectNodeIdAndRelevanceTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = Relevance)

        // S:1
        // - ST:1 (R:1)
        // - TST: 1-1 (R:1)
        // - ST:2 (R:2)
        // - TST:2-1 (R:2)
        // - ST:3 (R:1)
        // - TST:3-1 (R:1)
        // - TST:3-2 (R:1)
        // - TST:3-3 (R:2)

        // NOTE ST:3 does not have F:2 but should "inherit" it because one of the subtopics has F:2

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2, Relevance.SUPPLEMENTARY);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3, Relevance.CORE);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1, Relevance.SUPPLEMENTARY);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3, Relevance.SUPPLEMENTARY);
    }

    public Relevance recursiveNodesBySubjectNodeIdTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic)

        // S:1
        // - ST:1
        // - TST: 1-1
        // - ST:2
        // - TST:2-1
        // - ST:3
        // - TST:3-1
        // - TST:3-2
        // - TST:3-3

        clearAll();
        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2, Relevance.CORE);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3, Relevance.CORE);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3, Relevance.CORE);

        return Relevance.CORE;
    }

    public void resourceInDualSubjectsTestSetup() {
        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);
        final var subject2 = createNode(NodeType.SUBJECT, "urn:subject:2", "S:2", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "T:1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "T:2", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);
        createNodeConnection("urn:subject-topic:2", subject2, topic2, 1, Relevance.CORE);

        final var resource1 = createResource("urn:resource:1", "R:1", null);

        createNodeResource("urn:topic-resource:1", topic1, resource1, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:2", topic2, resource1, false, 1, Relevance.CORE);
    }

    public void resourceWithResourceTypeTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:1
        // - R:2

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.NODE, "urn:topic:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);

        createNodeResource("urn:topic-resource:1", topic1, resource1, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:2", topic1, resource2, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:3", topic1, resource3, true, 3, Relevance.CORE);

        final var resourceType1 = createResourceType(null, "urn:resourcetype:video", "Video");

        createResourceResourceType("urn:resource-resourcetype:1", resource1, resourceType1);
    }

    public void resourceWithRelevancesTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, R = resource, RC = core, RS = supplementary)
        //
        // S:1
        // - ST:1
        // - R:1 RC
        // - R:2 RC
        // - R:3 RC
        // - R:4 RC
        // - R:5 RC
        // - R:6 RC
        // - R:7 RC
        // - R:8 RC
        // - R:9 RC
        // - R:10 RS
        //

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);
        final var resource4 = createResource("urn:resource:4", "R:4", null);
        final var resource5 = createResource("urn:resource:5", "R:5", null);
        final var resource6 = createResource("urn:resource:6", "R:6", null);
        final var resource7 = createResource("urn:resource:7", "R:7", null);
        final var resource8 = createResource("urn:resource:8", "R:8", null);
        final var resource9 = createResource("urn:resource:9", "R:9", null);
        final var resource10 = createResource("urn:resource:10", "R:10", null);

        createNodeResource("urn:topic-resource:1", topic1, resource1, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:2", topic1, resource2, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:3", topic1, resource3, true, 3, Relevance.CORE);
        createNodeResource("urn:topic-resource:4", topic1, resource4, true, 4, Relevance.CORE);
        createNodeResource("urn:topic-resource:5", topic1, resource5, true, 5, Relevance.CORE);
        createNodeResource("urn:topic-resource:6", topic1, resource6, true, 6, Relevance.CORE);
        createNodeResource("urn:topic-resource:7", topic1, resource7, true, 7, Relevance.CORE);
        createNodeResource("urn:topic-resource:8", topic1, resource8, true, 8, Relevance.CORE);
        createNodeResource("urn:topic-resource:9", topic1, resource9, true, 9, Relevance.CORE);
        createNodeResource("urn:topic-resource:10", topic1, resource10, true, 10, Relevance.SUPPLEMENTARY);
    }

    public void resourceWithRelevancesButWithoutFiltersTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:1
        // - R:2
        //
        // New: Now with 100% less filters!: Split in two subjects based on the filters with
        // a cloned topic structure, but shared resources.

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "Year 1", null, true);
        final var subject2 = createNode(NodeType.SUBJECT, "urn:subject:2", "Year 2", null, true);

        final var f1topic1 = createNode(NodeType.TOPIC, "urn:topic:1:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:1", subject1, f1topic1, 1, Relevance.CORE);
        final var f2topic1 = createNode(NodeType.TOPIC, "urn:topic:2:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:2", subject2, f2topic1, 1, Relevance.CORE);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);
        final var resource4 = createResource("urn:resource:4", "R:4", null);
        final var resource5 = createResource("urn:resource:5", "R:5", null);
        final var resource6 = createResource("urn:resource:6", "R:6", null);
        final var resource7 = createResource("urn:resource:7", "R:7", null);
        final var resource8 = createResource("urn:resource:8", "R:8", null);
        final var resource9 = createResource("urn:resource:9", "R:9", null);
        final var resource10 = createResource("urn:resource:10", "R:10", null);

        createNodeResource("urn:topic-resource:1", f1topic1, resource1, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:2", f1topic1, resource2, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:3", f1topic1, resource3, true, 3, Relevance.CORE);
        createNodeResource("urn:topic-resource:4", f1topic1, resource4, true, 4, Relevance.CORE);
        createNodeResource("urn:topic-resource:5", f1topic1, resource5, true, 5, Relevance.CORE);
        createNodeResource("urn:topic-resource:6", f2topic1, resource1, true, 1, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:7", f2topic1, resource2, true, 2, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:8", f2topic1, resource3, true, 3, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:9", f2topic1, resource4, true, 4, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:10", f2topic1, resource5, true, 5, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:11", f2topic1, resource6, true, 6, Relevance.CORE);
        createNodeResource("urn:topic-resource:12", f2topic1, resource7, true, 7, Relevance.CORE);
        createNodeResource("urn:topic-resource:13", f2topic1, resource8, true, 8, Relevance.CORE);
        createNodeResource("urn:topic-resource:14", f2topic1, resource9, true, 9, Relevance.CORE);
        createNodeResource("urn:topic-resource:15", f2topic1, resource10, true, 10, Relevance.CORE);
    }

    public void resourceWithRelevancesAndOneNullRelevanceButWithoutFiltersTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:1
        // - R:2
        //
        // New: Now with 100% less filters!: Split in two subjects based on the filters with
        // a cloned topic structure, but shared resources.

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "Year 1", null, true);
        final var subject2 = createNode(NodeType.SUBJECT, "urn:subject:2", "Year 2", null, true);

        final var f1topic1 = createNode(NodeType.TOPIC, "urn:topic:1:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:1", subject1, f1topic1, 1, Relevance.CORE);
        final var f2topic1 = createNode(NodeType.TOPIC, "urn:topic:2:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:2", subject2, f2topic1, 1, Relevance.CORE);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);
        final var resource4 = createResource("urn:resource:4", "R:4", null);
        final var resource5 = createResource("urn:resource:5", "R:5", null);
        final var resource6 = createResource("urn:resource:6", "R:6", null);
        final var resource7 = createResource("urn:resource:7", "R:7", null);
        final var resource8 = createResource("urn:resource:8", "R:8", null);
        final var resource9 = createResource("urn:resource:9", "R:9", null);
        final var resource10 = createResource("urn:resource:10", "R:10", null);

        createNodeResource("urn:topic-resource:1", f1topic1, resource1, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:2", f1topic1, resource2, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:3", f1topic1, resource3, true, 3, Relevance.CORE);
        createNodeResource("urn:topic-resource:4", f1topic1, resource4, true, 4, Relevance.CORE);
        createNodeResource("urn:topic-resource:5", f1topic1, resource5, true, 5, Relevance.CORE);
        createNodeResource("urn:topic-resource:6", f2topic1, resource1, true, 1, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:7", f2topic1, resource2, true, 2, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:8", f2topic1, resource3, true, 3, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:9", f2topic1, resource4, true, 4, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:10", f2topic1, resource5, true, 5, Relevance.SUPPLEMENTARY);
        createNodeResource("urn:topic-resource:11", f2topic1, resource6, true, 6, Relevance.CORE);
        createNodeResource("urn:topic-resource:12", f2topic1, resource7, true, 7, Relevance.CORE);
        createNodeResource("urn:topic-resource:13", f2topic1, resource8, true, 8, Relevance.CORE);
        createNodeResource("urn:topic-resource:14", f2topic1, resource9, true, 9, Relevance.CORE);
        createNodeResource("urn:topic-resource:15", f2topic1, resource10, true, 10, Relevance.CORE);
    }

    public void resourcesBySubjectIdTestSetup() {
        // create a test structure with subjects, topics, subtopics and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:9 (F:1)
        // - TST: 1-1
        // - R:1 (F:1)
        // - ST:2
        // - TST:2-1
        // - R:2 (F:2)
        // - TST: 2-1-1
        // - R:10 (F:2)
        // - ST:3
        // - TST:3-1
        // - R:3 (F:1)
        // - R:5 (F:1)
        // - R:4 (F:2)
        // - TST:3-2
        // - R:6 (F:2)
        // - TST:3-3
        // - R:7 (F:1)
        // - R:8 (F:2)
        //

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);
        final var topic9 = createNode(NodeType.TOPIC, "urn:topic:9", "TST:2-1-1", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2, Relevance.CORE);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3, Relevance.CORE);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:6", topic4, topic9, 1, Relevance.CORE);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);
        final var resource4 = createResource("urn:resource:4", "R:4", null);
        final var resource5 = createResource("urn:resource:5", "R:5", null);
        final var resource6 = createResource("urn:resource:6", "R:6", null);
        final var resource7 = createResource("urn:resource:7", "R:7", null);
        final var resource8 = createResource("urn:resource:8", "R:8", null);
        final var resource9 = createResource("urn:resource:9", "R:9", null);
        final var resource10 = createResource("urn:resource:10", "R:10", null);

        createNodeResource("urn:topic-resource:1", topic2, resource1, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:2", topic4, resource2, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:3", topic6, resource3, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:4", topic6, resource4, true, 3, Relevance.CORE);
        createNodeResource("urn:topic-resource:5", topic6, resource5, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:6", topic7, resource6, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:7", topic8, resource7, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:8", topic8, resource8, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:9", topic1, resource9, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:10", topic9, resource10, true, 1, Relevance.CORE);
    }

    public void resourcesBySubjectNodeIdTestSetup() {
        // create a test structure with subjects, topics, subtopics and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:9 (F:1)
        // - TST: 1-1
        // - R:1 (F:1)
        // - ST:2
        // - TST:2-1
        // - R:2 (F:2)
        // - TST: 2-1-1
        // - R:10 (F:2)
        // - ST:3
        // - TST:3-1
        // - R:3 (F:1)
        // - R:5 (F:1)
        // - R:4 (F:2)
        // - TST:3-2
        // - R:6 (F:2)
        // - TST:3-3
        // - R:7 (F:1)
        // - R:8 (F:2)
        //

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);
        final var topic9 = createNode(NodeType.TOPIC, "urn:topic:9", "TST:2-1-1", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2, Relevance.CORE);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3, Relevance.CORE);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:6", topic4, topic9, 1, Relevance.CORE);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);
        final var resource4 = createResource("urn:resource:4", "R:4", null);
        final var resource5 = createResource("urn:resource:5", "R:5", null);
        final var resource6 = createResource("urn:resource:6", "R:6", null);
        final var resource7 = createResource("urn:resource:7", "R:7", null);
        final var resource8 = createResource("urn:resource:8", "R:8", null);
        final var resource9 = createResource("urn:resource:9", "R:9", null);
        final var resource10 = createResource("urn:resource:10", "R:10", null);

        createNodeResource("urn:topic-resource:1", topic2, resource1, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:2", topic4, resource2, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:3", topic6, resource3, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:4", topic6, resource4, true, 3, Relevance.CORE);
        createNodeResource("urn:topic-resource:5", topic6, resource5, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:6", topic7, resource6, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:7", topic8, resource7, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:8", topic8, resource8, true, 2, Relevance.CORE);
        createNodeResource("urn:topic-resource:9", topic1, resource9, true, 1, Relevance.CORE);
        createNodeResource("urn:topic-resource:10", topic9, resource10, true, 1, Relevance.CORE);
    }

    public void subtopicsByNodeIdAndRelevanceTestSetup() {
        // Creates subtopics with different filters
        //
        // Subjects S:1
        // \
        // \
        // Parent topic T1 (has filter F:1 and F:2)
        // |
        // Subtopics T1-1, T1-2, T1-3 (have filter F:1),
        // T1-4, T1-5, T1-6, T1-7 (have filter F:2)
        //

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "T1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "T1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "T1-2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "T1-3", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "T1-4", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "T1-5", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "T1-6", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "T1-7", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, Relevance.CORE);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:3", topic1, topic4, 3, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:2", topic1, topic3, 2, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:4", topic1, topic5, 4, Relevance.SUPPLEMENTARY);
        createNodeConnection("urn:topic-subtopic:5", topic1, topic6, 5, Relevance.SUPPLEMENTARY);
        createNodeConnection("urn:topic-subtopic:6", topic1, topic7, 6, Relevance.SUPPLEMENTARY);
        createNodeConnection("urn:topic-subtopic:7", topic1, topic8, 7, Relevance.SUPPLEMENTARY);
    }

    public void topicNodeConnectionsTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        //
        // S:1
        // \
        // T:1
        // \
        // T:2
        // / \
        // T:3 T:4
        //

        clearAll();
        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1000", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1000", "T1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2000", "T2", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3000", "T3", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4000", "T4", null, false);

        createNodeConnection("urn:subject-topic:1000", subject1, topic1, 1, Relevance.CORE);

        createNodeConnection("urn:topic-subtopic:1000", topic1, topic2, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:2000", topic2, topic3, 1, Relevance.CORE);
        createNodeConnection("urn:topic-subtopic:3000", topic2, topic4, 2, Relevance.CORE);
    }
}

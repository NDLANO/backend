/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Guards against N+1 query regressions on the hot read endpoints. The bounds are deliberately
 * generous; the point is that the query count must not scale with the number of nodes returned.
 */
public class NodesQueryCountTest extends RestTest {
    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
        resourceTypeRepository.deleteAllAndFlush();
        resourceResourceTypeRepository.deleteAllAndFlush();
    }

    private Statistics statistics() {
        var statistics = entityManager
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }

    /**
     * Flushes builder-created entities and empties the persistence context so the measured request
     * cannot be served from the first-level cache.
     */
    private Statistics freshStatistics() {
        entityManager.flush();
        entityManager.clear();
        var statistics = statistics();
        statistics.clear();
        return statistics;
    }

    @Test
    public void get_children_query_count_does_not_scale_with_child_count() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT, s -> {
            s.isContext(true).name("subject");
            for (int i = 0; i < 30; i++) {
                final var n = i;
                s.child(NodeType.TOPIC, t -> t.name("topic " + n).resource(r -> r.name("resource " + n)));
            }
        });

        var statistics = freshStatistics();
        var response = testUtils.getResource("/v1/nodes/" + subject.getPublicId() + "/nodes");
        var nodes = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(30, nodes.length);
        long queries = statistics.getPrepareStatementCount();
        assertTrue(queries >= 2, "statistics do not seem to measure anything, ran " + queries);
        assertTrue(queries <= 8, "expected getChildren to run at most 8 SQL statements, ran " + queries);
    }

    @Test
    public void recursive_get_children_query_count_does_not_scale_with_subtree_size() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT, s -> {
            s.isContext(true).name("subject");
            for (int i = 0; i < 15; i++) {
                final var n = i;
                s.child(
                        NodeType.TOPIC,
                        t -> t.name("topic " + n)
                                .child(
                                        NodeType.TOPIC,
                                        st -> st.name("subtopic " + n).resource(r -> r.name("resource " + n))));
            }
        });

        var statistics = freshStatistics();
        var response = testUtils.getResource(
                "/v1/nodes/" + subject.getPublicId() + "/nodes?recursive=true&nodeType=TOPIC,RESOURCE");
        var nodes = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(45, nodes.length);
        long queries = statistics.getPrepareStatementCount();
        assertTrue(queries >= 2, "statistics do not seem to measure anything, ran " + queries);
        assertTrue(queries <= 12, "expected recursive getChildren to run at most 12 SQL statements, ran " + queries);
    }

    @Test
    public void get_all_nodes_query_count_does_not_scale_with_node_count() throws Exception {
        builder.node(NodeType.SUBJECT, s -> {
            s.isContext(true).name("subject");
            for (int i = 0; i < 10; i++) {
                final var n = i;
                s.child(
                        NodeType.TOPIC,
                        t -> t.name("topic " + n)
                                .child(
                                        NodeType.TOPIC,
                                        st -> st.name("subtopic " + n).resource(r -> r.name("resource " + n))));
            }
        });

        var statistics = freshStatistics();
        var response = testUtils.getResource("/v1/nodes?nodeType=RESOURCE");
        var nodes = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(10, nodes.length);
        long queries = statistics.getPrepareStatementCount();
        assertTrue(queries >= 2, "statistics do not seem to measure anything, ran " + queries);
        assertTrue(queries <= 8, "expected getAllNodes to run at most 8 SQL statements, ran " + queries);
    }

    @Test
    public void get_all_nodes_returns_parent_crumbs_from_batched_ancestor_fetch() throws Exception {
        Node subject = builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true)
                        .name("subject")
                        .child(NodeType.TOPIC, t -> t.name("topic").resource(r -> r.name("resource"))));
        Node topic = subject.getChildNodes().iterator().next();

        freshStatistics();
        var response = testUtils.getResource("/v1/nodes?nodeType=RESOURCE");
        var nodes = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, nodes.length);
        var contexts = nodes[0].getContexts();
        assertEquals(1, contexts.size());
        var parents = contexts.getFirst().parents();
        assertEquals(2, parents.size());
        assertEquals(subject.getPublicId(), parents.get(0).id());
        assertEquals(topic.getPublicId(), parents.get(1).id());
        assertEquals(contexts.getFirst().rootId(), parents.get(0).id());
    }
}

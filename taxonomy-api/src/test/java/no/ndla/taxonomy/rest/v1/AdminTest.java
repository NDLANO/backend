/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.ndla.taxonomy.domain.Grade;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionPOST;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.transaction.TestTransaction;

public class AdminTest extends RestTest {

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
        resourceTypeRepository.deleteAllAndFlush();
        resourceResourceTypeRepository.deleteAllAndFlush();
    }

    public void connect(Node parent, Node child) throws Exception {
        var connectBody = new NodeConnectionPOST();
        connectBody.parentId = parent.getPublicId();
        connectBody.childId = child.getPublicId();

        testUtils.createResource("/v1/node-connections/", connectBody);
    }

    public void disconnect(Node parent, Node child) throws Exception {
        var connection = nodeConnectionRepository.findByParentIdAndChildId(parent.getId(), child.getId());
        var connectionId = connection.getPublicId();
        testUtils.deleteResource("/v1/node-connections/" + connectionId);
    }

    public void testQualityEvaluationAverage(Node inputNode, int expectedCount, double expectedAverage) {
        var node = nodeRepository.findFirstByPublicId(inputNode.getPublicId()).orElseThrow();
        var qe = node.getChildQualityEvaluationAverage().orElseThrow();
        assertEquals(expectedCount, qe.getCount());
        assertEquals(expectedAverage, qe.getAverageValue());
    }

    public void assertMissingQualityEvaluation(Node inputNode) {
        var node = nodeRepository.findFirstByPublicId(inputNode.getPublicId()).orElseThrow();
        var qe = node.getChildQualityEvaluationAverage();
        assertTrue(qe.isEmpty());
    }

    @Test
    public void rebuilding_quality_evaluation_works_as_expected() throws Exception {
        var s1 = builder.node(NodeType.SUBJECT, s -> s.name("S1").publicId("urn:subject:1"));
        var s2 = builder.node(NodeType.SUBJECT, s -> s.name("S2").publicId("urn:subject:2"));

        var t1 = builder.node(
                NodeType.TOPIC, n -> n.name("T1").publicId("urn:topic:1").qualityEvaluation(Grade.Four));
        var t2 = builder.node(NodeType.TOPIC, n -> n.name("T2").qualityEvaluation(Grade.One));
        var t3 = builder.node(NodeType.TOPIC, n -> n.name("T3").qualityEvaluation(Grade.Two));
        var t4 = builder.node(
                NodeType.TOPIC, n -> n.name("T4").publicId("urn:topic:4").qualityEvaluation(Grade.One));

        var t5 = builder.node(
                NodeType.TOPIC, n -> n.name("T5").publicId("urn:topic:5").qualityEvaluation(Grade.One));

        var r1 = builder.node(NodeType.RESOURCE, n -> n.name("R1").qualityEvaluation(Grade.Five));
        var r2 = builder.node(NodeType.RESOURCE, n -> n.name("R2").qualityEvaluation(Grade.Five));
        var r3 = builder.node(NodeType.RESOURCE, n -> n.name("R3").qualityEvaluation(Grade.Three));
        var r4 = builder.node(NodeType.RESOURCE, n -> n.name("R4").qualityEvaluation(Grade.Four));
        var r5 = builder.node(NodeType.RESOURCE, n -> n.name("R5").qualityEvaluation(Grade.One));
        var r6 = builder.node(NodeType.RESOURCE, n -> n.name("R6").qualityEvaluation(Grade.Five));
        var r7 = builder.node(NodeType.RESOURCE, n -> n.name("R7").qualityEvaluation(Grade.Five));
        var r8 = builder.node(NodeType.RESOURCE, n -> n.name("R8").qualityEvaluation(Grade.Four));
        var r9 = builder.node(NodeType.RESOURCE, n -> n.name("R9").qualityEvaluation(Grade.Four));
        var r10 = builder.node(NodeType.RESOURCE, n -> n.name("R10").qualityEvaluation(Grade.Five));
        var r11 = builder.node(NodeType.RESOURCE, n -> n.name("R11").qualityEvaluation(Grade.Three));
        var r12 = builder.node(NodeType.RESOURCE, n -> n.name("R12").qualityEvaluation(Grade.Two));
        var r13 = builder.node(NodeType.RESOURCE, n -> n.name("R13").qualityEvaluation(Grade.Five));
        var r14 = builder.node(NodeType.RESOURCE, n -> n.name("R14").qualityEvaluation(Grade.One));
        var r15 = builder.node(NodeType.RESOURCE, n -> n.name("R15").qualityEvaluation(Grade.Four));
        var r16 = builder.node(NodeType.RESOURCE, n -> n.name("R16").qualityEvaluation(Grade.Four));

        connect(s1, t1);
        connect(t1, t2);
        connect(t2, t3);

        connect(t5, r9);
        connect(t5, r10);
        connect(t5, r11);
        connect(t5, r12);

        connect(s2, t5);

        connect(t1, r1);
        connect(t1, r2);
        connect(t1, r3);
        connect(t1, r4);

        connect(t2, r5);
        connect(t2, r6);
        connect(t2, r7);
        connect(t2, r8);

        connect(t3, r9);
        connect(t3, r10);
        connect(t3, r11);
        connect(t3, r12);

        connect(t4, r13);
        connect(t4, r14);
        connect(t4, r15);
        connect(t4, r16);

        connect(t1, t4);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        testQualityEvaluationAverage(s1, 16, 3.75);
        testQualityEvaluationAverage(t1, 16, 3.75);
        testQualityEvaluationAverage(t2, 8, 3.625);
        testQualityEvaluationAverage(t3, 4, 3.5);
        testQualityEvaluationAverage(t4, 4, 3.5);
        testQualityEvaluationAverage(s2, 4, 3.5);
        testQualityEvaluationAverage(t5, 4, 3.5);

        nodeRepository.wipeQualityEvaluationAverages();
        nodeRepository.flush();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertMissingQualityEvaluation(s1);
        assertMissingQualityEvaluation(t1);
        assertMissingQualityEvaluation(t2);
        assertMissingQualityEvaluation(t3);
        assertMissingQualityEvaluation(t4);

        testUtils.createResource("/v1/admin/buildAverageTree");

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        testQualityEvaluationAverage(s1, 16, 3.75);
        testQualityEvaluationAverage(t1, 16, 3.75);
        testQualityEvaluationAverage(t2, 8, 3.625);
        testQualityEvaluationAverage(t3, 4, 3.5);
        testQualityEvaluationAverage(t4, 4, 3.5);
        testQualityEvaluationAverage(s2, 4, 3.5);
        testQualityEvaluationAverage(t5, 4, 3.5);

        disconnect(t1, t4);

        testQualityEvaluationAverage(s1, 12, 3.8333333333333335);
        testQualityEvaluationAverage(t1, 12, 3.8333333333333335);
    }
}

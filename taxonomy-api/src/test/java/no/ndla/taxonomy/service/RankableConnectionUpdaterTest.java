/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;

public class RankableConnectionUpdaterTest {
    private void verifyOrder(List<NodeConnection> list, List<String> expectedOrder) {
        assertEquals(expectedOrder.size(), list.size());

        for (var index = 0; index < expectedOrder.size(); index++) {
            assertEquals(expectedOrder.get(index), list.get(index).getPublicId().toString());
        }
    }

    @Test
    public void rank() {
        final var relevance = Relevance.CORE;

        final var rankable1 = new NodeConnection("urn:1", relevance, 0);
        final var rankable2 = new NodeConnection("urn:2", relevance, 1);
        final var rankable3 = new NodeConnection("urn:3", relevance, 10);
        final var rankable4 = new NodeConnection("urn:4", relevance, 11);
        final var rankable5 = new NodeConnection("urn:5", relevance, 12);
        final var rankable6 = new NodeConnection("urn:6", relevance, 20);
        final var rankable7 = new NodeConnection("urn:7", relevance, 100);
        final var rankable8 = new NodeConnection("urn:8", relevance, 1000);

        final var rankableList = new ArrayList<NodeConnection>();

        RankableConnectionUpdater.rank(rankableList, rankable1, 0);
        RankableConnectionUpdater.rank(rankableList, rankable2, 1);
        RankableConnectionUpdater.rank(rankableList, rankable3, 10);
        RankableConnectionUpdater.rank(rankableList, rankable4, 11);
        RankableConnectionUpdater.rank(rankableList, rankable5, 12);
        RankableConnectionUpdater.rank(rankableList, rankable6, 20);
        RankableConnectionUpdater.rank(rankableList, rankable7, 100);

        assertEquals(0, rankable1.getRank());
        assertEquals(1, rankable2.getRank());
        assertEquals(10, rankable3.getRank());
        assertEquals(11, rankable4.getRank());
        assertEquals(12, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        verifyOrder(rankableList, List.of("urn:1", "urn:2", "urn:3", "urn:4", "urn:5", "urn:6", "urn:7"));

        RankableConnectionUpdater.rank(rankableList, rankable3, 0);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:2", "urn:4", "urn:5", "urn:6", "urn:7"));

        assertEquals(1, rankable1.getRank());
        assertEquals(2, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(11, rankable4.getRank());
        assertEquals(12, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        RankableConnectionUpdater.rank(rankableList, rankable2, 200);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:4", "urn:5", "urn:6", "urn:7", "urn:2"));

        assertEquals(1, rankable1.getRank());
        assertEquals(200, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(11, rankable4.getRank());
        assertEquals(12, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        RankableConnectionUpdater.rank(rankableList, rankable2, 11);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:2", "urn:4", "urn:5", "urn:6", "urn:7"));

        assertEquals(1, rankable1.getRank());
        assertEquals(11, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(12, rankable4.getRank());
        assertEquals(13, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        RankableConnectionUpdater.rank(rankableList, rankable8, 11);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:8", "urn:2", "urn:4", "urn:5", "urn:6", "urn:7"));

        assertEquals(1, rankable1.getRank());
        assertEquals(12, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(13, rankable4.getRank());
        assertEquals(14, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(11, rankable8.getRank());
    }

    @Test
    public void rank_cascading() {
        final var relevance = Relevance.CORE;

        final var rankable1 = new NodeConnection("urn:1", relevance, 0);
        final var rankable2 = new NodeConnection("urn:2", relevance, 1);
        final var rankable3 = new NodeConnection("urn:3", relevance, 10);
        final var rankable4 = new NodeConnection("urn:4", relevance, 10);
        final var rankable5 = new NodeConnection("urn:5", relevance, 10);
        final var rankable6 = new NodeConnection("urn:6", relevance, 10);
        final var rankable7 = new NodeConnection("urn:7", relevance, 13);
        final var rankable8 = new NodeConnection("urn:8", relevance, 20);

        final var rankableList = Arrays.nonNullElementsIn(new NodeConnection[] {
            rankable1, rankable2, rankable3, rankable4, rankable5, rankable6, rankable7, rankable8
        });

        assertEquals(0, rankable1.getRank());
        assertEquals(1, rankable2.getRank());
        assertEquals(10, rankable3.getRank());
        assertEquals(10, rankable4.getRank());
        assertEquals(10, rankable5.getRank());
        assertEquals(10, rankable6.getRank());
        assertEquals(13, rankable7.getRank());
        assertEquals(20, rankable8.getRank());

        verifyOrder(rankableList, List.of("urn:1", "urn:2", "urn:3", "urn:4", "urn:5", "urn:6", "urn:7", "urn:8"));

        RankableConnectionUpdater.rank(rankableList, rankable5, 10);
        verifyOrder(rankableList, List.of("urn:1", "urn:2", "urn:5", "urn:3", "urn:4", "urn:6", "urn:7", "urn:8"));

        assertEquals(0, rankable1.getRank());
        assertEquals(1, rankable2.getRank());
        assertEquals(11, rankable3.getRank());
        assertEquals(12, rankable4.getRank());
        assertEquals(10, rankable5.getRank());
        assertEquals(13, rankable6.getRank());
        assertEquals(14, rankable7.getRank());
        assertEquals(20, rankable8.getRank());
    }
}

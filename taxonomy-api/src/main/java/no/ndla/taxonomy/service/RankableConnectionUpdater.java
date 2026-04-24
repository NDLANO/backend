/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.util.Comparator;
import java.util.List;
import no.ndla.taxonomy.domain.NodeConnection;

public class RankableConnectionUpdater {

    public static List<NodeConnection> rank(
            List<NodeConnection> existingConnections, NodeConnection updatedConnection, int desiredRank) {
        updatedConnection.setRank(desiredRank);
        if (!existingConnections.isEmpty()) {
            existingConnections.removeIf(
                    subjectTopic -> subjectTopic.getPublicId().equals(updatedConnection.getPublicId()));
            existingConnections.sort(Comparator.comparingInt(NodeConnection::getRank));
            int newIndex = insertInRankOrder(existingConnections, updatedConnection);
            if (!connectionWasInsertedAtEnd(newIndex)) {
                updateAdjacentRankedConnections(existingConnections, updatedConnection, newIndex + 1);
            }
        } else {
            existingConnections.add(updatedConnection);
        }
        return existingConnections;
    }

    private static boolean connectionWasInsertedAtEnd(int insertedAtIndex) {
        return insertedAtIndex == -1;
    }

    private static void updateAdjacentRankedConnections(
            List<NodeConnection> existingConnections, NodeConnection updatedConnection, int startFromIndex) {
        int lastUpdatedConnectionRank = updatedConnection.getRank();
        for (int i = startFromIndex; i < existingConnections.size(); i++) {
            NodeConnection currentItem = existingConnections.get(i);
            int currentRank = currentItem.getRank();
            if (currentRank <= lastUpdatedConnectionRank) {
                currentItem.setRank(lastUpdatedConnectionRank + 1);
                lastUpdatedConnectionRank += 1;
            } else return;
        }
    }

    /**
     * @return the index the connectionToRank was inserted at, or -1 if it was inserted at the end
     */
    private static int insertInRankOrder(List<NodeConnection> existingConnections, NodeConnection connectionToRank) {
        for (int i = 0; i < existingConnections.size(); i++) {
            if (existingConnections.get(i).getRank() >= connectionToRank.getRank()) {
                existingConnections.add(i, connectionToRank);
                return i;
            }
        }
        existingConnections.add(connectionToRank);
        return -1;
    }
}

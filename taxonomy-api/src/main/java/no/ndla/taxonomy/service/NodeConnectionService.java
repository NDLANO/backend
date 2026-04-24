/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import no.ndla.taxonomy.domain.*;

public interface NodeConnectionService {
    NodeConnection connectParentChild(
            Node parent,
            Node child,
            Relevance relevance,
            Integer rank,
            Optional<Boolean> isPrimary,
            NodeConnectionType connectionType);

    void disconnectParentChild(Node parent, Node child);

    void disconnectParentChildConnection(NodeConnection nodeConnection);

    void disconnectAllParents(URI nodeId);

    void disconnectAllParents(Node entity);

    void updateParentChild(
            NodeConnection nodeConnection, Relevance relevance, Optional<Integer> newRank, Optional<Boolean> isPrimary);

    void replacePrimaryConnectionsFor(Node entity);

    Collection<NodeConnection> getParentConnections(Node entity);

    Collection<NodeConnection> getChildConnections(Node entity);

    void disconnectAllChildren(Node entity);

    Optional<DomainEntity> disconnectAllInvisibleNodes();
}

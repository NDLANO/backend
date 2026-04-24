/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.*;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
   This class replicates old structure previously implemented as recursive queries in database, methods
   generally just returns flat lists that replicates the old database views
*/
@Transactional(propagation = Propagation.MANDATORY)
@Service
public class RecursiveNodeTreeService {
    private final NodeConnectionRepository nodeConnectionRepository;

    public RecursiveNodeTreeService(NodeConnectionRepository nodeConnectionRepository) {
        this.nodeConnectionRepository = nodeConnectionRepository;
    }

    private void addChildIdsRecursively(Set<TreeElement> elements, Set<URI> ids, int ttl, List<NodeType> nodeTypes) {
        // Method just takes the list of ids provided and add each of the children it finds to the list,
        // and then recursively runs the same method on each of the found children IDs, once for each level

        if (--ttl < 0) {
            throw new IllegalStateException("Recursion limit reached, probably an infinite loop in the structure");
        }

        final var idsThisLevel = new HashSet<URI>();

        nodeConnectionRepository
                .findAllByNodeIdInIncludingTopicAndSubtopic(ids, nodeTypes)
                .forEach(nodeConnection -> {
                    var child = nodeConnection.getChild();
                    var parent = nodeConnection.getParent();
                    if (child.isEmpty() || parent.isEmpty()) return;

                    if (nodeConnection.getConnectionType() != NodeConnectionType.BRANCH) return;

                    var childId = child.get().getPublicId();
                    var parentId = parent.get().getPublicId();

                    elements.add(new TreeElement(childId, parentId, nodeConnection.getRank()));
                    idsThisLevel.add(childId);
                });

        if (!idsThisLevel.isEmpty()) {
            addChildIdsRecursively(elements, idsThisLevel, ttl, nodeTypes);
        }
    }

    public Set<TreeElement> getRecursiveNodes(Node node, List<NodeType> nodeTypes) {
        final var toReturn = new HashSet<TreeElement>();
        toReturn.add(new TreeElement(node.getPublicId(), null, 0));

        addChildIdsRecursively(toReturn, Set.of(node.getPublicId()), 1000, nodeTypes);

        return toReturn;
    }

    public Set<TreeElement> getRecursiveNodes(Node node) {
        var defaultNodeTypes = NodeType.values();
        return getRecursiveNodes(node, Arrays.stream(defaultNodeTypes).toList());
    }

    public static class TreeElement {
        private final URI id;
        private final URI parentId;
        private final int rank;

        public TreeElement(URI id, URI parentId, int rank) {
            this.id = id;
            this.parentId = parentId;
            this.rank = rank;
        }

        public URI getId() {
            return id;
        }

        public Optional<URI> getParentId() {
            return Optional.ofNullable(parentId);
        }

        public int getRank() {
            return rank;
        }
    }
}

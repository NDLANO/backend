/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodeResourceTest {
    private Node node;
    private Node resource;
    private NodeConnection resourceConnection;

    @BeforeEach
    public void setUp() {
        node = mock(Node.class);
        when(node.getNodeType()).thenReturn(NodeType.TOPIC);
        resource = mock(Node.class);
        when(resource.getNodeType()).thenReturn(NodeType.RESOURCE);
        Relevance core = mock(Relevance.class);

        resourceConnection = NodeConnection.create(node, resource, core, NodeConnectionType.BRANCH, false);

        verify(node).addChildConnection(resourceConnection);
        verify(resource).addParentConnection(resourceConnection);

        assertNotNull(resourceConnection.getPublicId());
        assertTrue(resourceConnection.getPublicId().toString().length() > 4);
    }

    @Test
    public void getNode() {
        assertSame(node, resourceConnection.getParent().orElse(null));
    }

    @Test
    public void getResource() {
        assertSame(resource, resourceConnection.getResource().orElse(null));
    }

    @Test
    public void setAndIsPrimary() {
        assertFalse(resourceConnection.isPrimary().orElseThrow());
        resourceConnection.setPrimary(true);
        assertTrue(resourceConnection.isPrimary().orElseThrow());
        resourceConnection.setPrimary(false);
        assertFalse(resourceConnection.isPrimary().orElseThrow());
    }

    @Test
    public void setAndGetRank() {
        assertEquals(0, resourceConnection.getRank());
        resourceConnection.setRank(100);
        assertEquals(100, resourceConnection.getRank());
    }

    @Test
    public void preRemove() {
        resourceConnection.preRemove();

        assertFalse(resourceConnection.getResource().isPresent());
        assertFalse(resourceConnection.getParent().isPresent());

        verify(node).removeChildConnection(resourceConnection);
        verify(resource).removeParentConnection(resourceConnection);
    }
}

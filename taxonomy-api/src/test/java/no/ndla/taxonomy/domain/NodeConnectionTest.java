/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodeConnectionTest {
    private Node parent;
    private Node child;

    private NodeConnection connection;

    @BeforeEach
    public void setUp() {
        parent = mock(Node.class);
        child = mock(Node.class);
        Relevance core = mock(Relevance.class);

        connection = NodeConnection.create(parent, child, core);
        assertNotNull(connection.getPublicId());
    }

    @Test
    public void getParent() {
        assertSame(parent, connection.getParent().orElse(null));
    }

    @Test
    public void getChild() {
        assertSame(child, connection.getChild().orElse(null));
    }

    @Test
    public void getAndSetRank() {
        assertEquals(0, connection.getRank());
        connection.setRank(10);
        assertEquals(10, connection.getRank());
    }

    @Test
    public void preRemove() {
        connection.preRemove();

        verify(parent).removeChildConnection(connection);
        verify(child).removeParentConnection(connection);
        assertFalse(connection.getParent().isPresent());
        assertFalse(connection.getChild().isPresent());
    }
}

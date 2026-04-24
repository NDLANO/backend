/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import org.junit.jupiter.api.Test;

public class ResourceTreeSortableTest {
    @Test
    public void getSortableRank() {

        // TopicResource
        var sortable = new ResourceTreeSortable("resource", "topic", URI.create("100"), URI.create("10"), 1000);
        assertEquals(1000 - 10000, sortable.getSortableRank());

        // TopicSubtopic
        sortable = new ResourceTreeSortable("topic", "topic", URI.create("100"), URI.create("10"), 800);
        assertEquals(800 - 1000, sortable.getSortableRank());
    }

    @Test
    public void getSortableId() throws URISyntaxException {
        final var sortable = new ResourceTreeSortable("resource", "topic", URI.create("100"), URI.create("10"), 1000);
        assertEquals(new URI("urn:resource:100"), sortable.getSortableId());
    }

    @Test
    public void getSortableParentId() throws URISyntaxException {
        final var sortable = new ResourceTreeSortable("resource", "topic", URI.create("100"), URI.create("10"), 1000);
        assertEquals(new URI("urn:topic:10"), sortable.getSortableParentId());
    }

    @Test
    public void getNodeResource() {
        var sortable = new ResourceTreeSortable("resource", "topic", URI.create("100"), URI.create("10"), 1000);
        assertFalse(sortable.getResourceConnection().isPresent());

        final var node = mock(Node.class);
        final var resource = mock(Node.class);
        final var nodeResource = mock(NodeConnection.class);

        when(node.getId()).thenReturn(101);
        when(resource.getId()).thenReturn(102);

        when(nodeResource.getParent()).thenReturn(Optional.of(node));
        when(nodeResource.getResource()).thenReturn(Optional.of(resource));

        when(nodeResource.getRank()).thenReturn(200 - 10000);

        sortable = new ResourceTreeSortable(nodeResource);
        assertSame(nodeResource, sortable.getResourceConnection().orElse(null));
    }

    @Test
    public void testPopulateFromNodeResource() throws URISyntaxException {
        final var node = mock(Node.class);
        final var resource = mock(Node.class);
        final var nodeResource = mock(NodeConnection.class);

        when(node.getPublicId()).thenReturn(URI.create("101"));
        when(resource.getPublicId()).thenReturn(URI.create("102"));

        when(nodeResource.getParent()).thenReturn(Optional.of(node));
        when(nodeResource.getResource()).thenReturn(Optional.of(resource));

        when(nodeResource.getRank()).thenReturn(200);

        final var sortable = new ResourceTreeSortable(nodeResource);

        assertEquals(200 - 10000, sortable.getSortableRank());
        assertEquals(new URI("urn:node:101"), sortable.getSortableParentId());
        assertEquals(new URI("urn:resource:102"), sortable.getSortableId());
    }
}

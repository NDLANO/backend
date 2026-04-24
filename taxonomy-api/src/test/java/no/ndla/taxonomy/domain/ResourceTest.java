/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResourceTest {
    private Node resource;

    @BeforeEach
    public void setUp() {
        this.resource = new Node(NodeType.RESOURCE);
    }

    @Test
    public void addAndGetAndRemoveTranslations() {
        assertEquals(0, resource.getTranslations().size());

        var returnedTranslation = resource.addTranslation("hei", "nb");
        assertEquals(1, resource.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(resource.getTranslations().contains(returnedTranslation));
        assertEquals(resource, returnedTranslation.getParent());

        var returnedTranslation2 = resource.addTranslation("hello", "en");
        assertEquals(2, resource.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(resource.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(resource, returnedTranslation2.getParent());

        resource.removeTranslation("nb");

        assertNull(returnedTranslation.getParent());
        assertFalse(resource.getTranslations().contains(returnedTranslation));

        assertFalse(resource.getTranslation("nb").isPresent());

        resource.addTranslation(returnedTranslation);
        assertEquals(resource, returnedTranslation.getParent());
        assertTrue(resource.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, resource.getTranslation("nb").get());
        assertEquals(returnedTranslation2, resource.getTranslation("en").get());
    }

    @Test
    public void getAndSetName() {
        assertNull(resource.getName());
        resource.setName("test name");
        assertEquals("test name", resource.getName());
    }

    @Test
    public void getNodes() {
        final var node1 = mock(Node.class);
        final var node2 = mock(Node.class);

        assertEquals(0, resource.getParentConnections().size());

        final var nodeResource1 = mock(NodeConnection.class);
        final var nodeResource2 = mock(NodeConnection.class);

        when(nodeResource1.getParent()).thenReturn(Optional.of(node1));
        when(nodeResource2.getParent()).thenReturn(Optional.of(node2));

        setField(resource, "parentConnections", Set.of(nodeResource1, nodeResource2));

        assertEquals(2, resource.getParentNodes().size());
        assertTrue(resource.getParentNodes().containsAll(Set.of(node1, node2)));
    }

    @Test
    public void getAddAndRemoveResourceTypes() {
        final var resourceType1 = mock(ResourceType.class);
        final var resourceType2 = mock(ResourceType.class);

        when(resourceType1.getId()).thenReturn(100);
        when(resourceType2.getId()).thenReturn(101);

        assertEquals(0, resource.getResourceTypes().size());

        resource.addResourceType(resourceType1);
        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType1));
        resource.addResourceType(resourceType2);
        assertEquals(2, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().containsAll(Set.of(resourceType1, resourceType2)));

        try {
            resource.addResourceType(resourceType2);
            fail("Expected DuplicateIdException");
        } catch (DuplicateIdException ignored) {

        }

        resource.removeResourceType(resourceType1);
        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType2));
        resource.removeResourceType(resourceType2);
        assertEquals(0, resource.getResourceTypes().size());
    }

    @Test
    public void addRemoveAndGetResourceResourceType() {
        final var resourceResourceType1 = mock(ResourceResourceType.class);
        final var resourceResourceType2 = mock(ResourceResourceType.class);

        assertEquals(0, resource.getResourceResourceTypes().size());

        try {
            resource.addResourceResourceType(resourceResourceType1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }

        when(resourceResourceType1.getNode()).thenReturn(resource);
        resource.addResourceResourceType(resourceResourceType1);

        assertEquals(1, resource.getResourceResourceTypes().size());
        assertTrue(resource.getResourceResourceTypes().contains(resourceResourceType1));

        try {
            resource.addResourceResourceType(resourceResourceType2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(resourceResourceType2.getNode()).thenReturn(resource);
        resource.addResourceResourceType(resourceResourceType2);

        assertEquals(2, resource.getResourceResourceTypes().size());
        assertTrue(
                resource.getResourceResourceTypes().containsAll(Set.of(resourceResourceType1, resourceResourceType2)));

        reset(resourceResourceType1);
        reset(resourceResourceType2);

        when(resourceResourceType1.getNode()).thenReturn(resource);
        when(resourceResourceType2.getNode()).thenReturn(resource);

        resource.removeResourceResourceType(resourceResourceType1);
        assertEquals(1, resource.getResourceResourceTypes().size());
        assertTrue(resource.getResourceResourceTypes().contains(resourceResourceType2));
        verify(resourceResourceType1).disassociate();

        resource.removeResourceResourceType(resourceResourceType2);
        assertEquals(0, resource.getResourceResourceTypes().size());
        verify(resourceResourceType2).disassociate();
    }

    @Test
    public void setAndGetContentUri() throws URISyntaxException {
        assertNull(resource.getContentUri());

        final var uri1 = new URI("urn:test1");
        final var uri2 = new URI("urn:test2");

        resource.setContentUri(uri1);
        assertEquals("urn:test1", resource.getContentUri().toString());

        resource.setContentUri(uri2);
        assertEquals("urn:test2", resource.getContentUri().toString());
    }

    @Test
    public void addAndRemoveResourceType() {
        assertEquals(0, resource.getResourceTypes().size());

        final var resourceType1 = mock(ResourceType.class);
        resource.addResourceType(resourceType1);

        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType1));

        final var resourceType2 = mock(ResourceType.class);
        resource.addResourceType(resourceType2);

        assertEquals(2, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().containsAll(Set.of(resourceType1, resourceType2)));

        resource.removeResourceType(resourceType1);
        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType2));

        resource.removeResourceType(resourceType2);
        assertEquals(0, resource.getResourceTypes().size());
    }

    @Test
    public void addGetAndRemoveNodeResources() {
        final var nodeResource1 = mock(NodeConnection.class);
        final var nodeResource2 = mock(NodeConnection.class);

        assertEquals(0, resource.getParentConnections().size());

        try {
            resource.addParentConnection(nodeResource1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(nodeResource1.getChild()).thenReturn(Optional.of(resource));
        resource.addParentConnection(nodeResource1);

        assertEquals(1, resource.getParentConnections().size());
        assertTrue(resource.getParentConnections().contains(nodeResource1));

        try {
            resource.addParentConnection(nodeResource2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(nodeResource2.getChild()).thenReturn(Optional.of(resource));
        resource.addParentConnection(nodeResource2);

        assertEquals(2, resource.getParentConnections().size());
        assertTrue(resource.getParentConnections().containsAll(Set.of(nodeResource1, nodeResource2)));

        when(nodeResource1.getChild()).thenReturn(Optional.of(resource));
        when(nodeResource2.getChild()).thenReturn(Optional.of(resource));

        resource.removeParentConnection(nodeResource1);

        verify(nodeResource1).disassociate();

        assertEquals(1, resource.getParentConnections().size());
        assertTrue(resource.getParentConnections().contains(nodeResource2));

        resource.removeParentConnection(nodeResource2);

        verify(nodeResource2).disassociate();
        assertEquals(0, resource.getParentConnections().size());
    }

    @Test
    public void preRemove() {
        final var nodeResource1 = mock(NodeConnection.class);
        final var nodeResource2 = mock(NodeConnection.class);

        Set.of(nodeResource1, nodeResource2).forEach(nodeResource -> {
            when(nodeResource.getChild()).thenReturn(Optional.of(resource));
            resource.addParentConnection(nodeResource);
        });

        resource.preRemove();

        verify(nodeResource1).disassociate();
        verify(nodeResource2).disassociate();
    }

    @Test
    public void isContext() {
        final var resource = new Node(NodeType.RESOURCE);
        assertFalse(resource.isContext());
    }
}

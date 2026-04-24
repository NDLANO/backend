/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResourceResourceTypeTest {
    private Node resource;
    private ResourceType resourceType;
    private ResourceResourceType resourceResourceType;

    @BeforeEach
    public void setUp() {
        resource = mock(Node.class);
        resourceType = mock(ResourceType.class);
        resourceResourceType = ResourceResourceType.create(resource, resourceType);

        verify(resource).addResourceResourceType(resourceResourceType);
    }

    @Test
    public void getResource() {
        assertSame(resource, resourceResourceType.getNode());
    }

    @Test
    public void getResourceType() {
        assertSame(resourceType, resourceResourceType.getResourceType());
    }
}

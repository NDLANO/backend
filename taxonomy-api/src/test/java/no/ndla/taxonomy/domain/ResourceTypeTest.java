/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResourceTypeTest {
    private ResourceType resourceType;

    @BeforeEach
    public void setUp() {
        resourceType = new ResourceType();
    }

    @Test
    public void testConstructor() {
        final var createdResourceType = new ResourceType();
        assertNotNull(createdResourceType.getPublicId());
        assertTrue(createdResourceType.getPublicId().toString().length() > 4);
    }

    @Test
    public void name() {
        resourceType.setName("testname");
        assertEquals("testname", resourceType.getName());
    }

    @Test
    public void setAndGetParent() {
        final var parent = new ResourceType();

        assertNull(resourceType.getParent());
        resourceType.setParent(parent);
        assertEquals(parent, resourceType.getParent());
        resourceType.setParent(null);
        assertNull(resourceType.getParent());
    }

    @Test
    public void getAddAndRemoveTranslation() {
        assertEquals(0, resourceType.getTranslations().size());

        var returnedTranslation = resourceType.addTranslation("hei", "nb");
        assertEquals(1, resourceType.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(resourceType.getTranslations().contains(returnedTranslation));

        var returnedTranslation2 = resourceType.addTranslation("hello", "en");
        assertEquals(2, resourceType.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(resourceType.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));

        resourceType.removeTranslation("nb");

        assertFalse(resourceType.getTranslations().contains(returnedTranslation));

        assertFalse(resourceType.getTranslation("nb").isPresent());

        resourceType.addTranslation(returnedTranslation);
        assertTrue(resourceType.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, resourceType.getTranslation("nb").get());
        assertEquals(returnedTranslation2, resourceType.getTranslation("en").orElse(null));
    }
}

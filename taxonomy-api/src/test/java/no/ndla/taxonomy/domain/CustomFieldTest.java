/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomFieldTest {
    private CustomField customField;

    @BeforeEach
    public void setUp() {
        customField = new CustomField();
    }

    @Test
    public void getId() {
        final var id = 1;
        setField(customField, "id", id);
        assertEquals(id, customField.getId());
    }

    @Test
    public void setId() {
        final var id = 1;
        customField.setId(id);
        assertEquals(id, getField(customField, "id"));
    }

    @Test
    public void getKey() {
        setField(customField, "key", "key");
        assertEquals("key", customField.getKey());
    }

    @Test
    public void setKey() {
        customField.setKey("key");
        assertEquals("key", getField(customField, "key"));
    }
}

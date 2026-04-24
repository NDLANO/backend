/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UriTypeConverterTest {
    private UriTypeConverter uriTypeConverter;

    @BeforeEach
    void setUp() {
        uriTypeConverter = new UriTypeConverter();
    }

    @Test
    void convertToDatabaseColumn() {
        assertNull(uriTypeConverter.convertToDatabaseColumn(null));
        assertEquals("urn:test:1", uriTypeConverter.convertToDatabaseColumn(URI.create("urn:test:1")));
    }

    @Test
    void convertToEntityAttribute() {
        assertNull(uriTypeConverter.convertToEntityAttribute(null));
        assertEquals(URI.create("urn:test:1"), uriTypeConverter.convertToEntityAttribute("urn:test:1"));
    }
}

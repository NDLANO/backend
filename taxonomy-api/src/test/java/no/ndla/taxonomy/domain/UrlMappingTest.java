/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UrlMappingTest {
    private UrlMapping urlMapping;

    @BeforeEach
    public void setUp() {
        urlMapping = new UrlMapping("urn:test:0", URI.create("urn:test-public-0"), URI.create("urn:test-subject-id-0"));
    }

    @Test
    public void setAndGetOldUrl() {
        urlMapping.setOldUrl("urn:test-1");
        assertEquals("urn:test-1", urlMapping.getOldUrl());
    }

    @Test
    public void setAndGetPublic_id() throws URISyntaxException {
        urlMapping.setPublicId(new URI("urn:test-public-1"));
        assertEquals("urn:test-public-1", urlMapping.getPublicId().toString());
    }

    @Test
    public void getAndSetSubject_id() throws URISyntaxException {
        urlMapping.setSubjectId(new URI("urn:test-subject-id-1"));
        assertEquals("urn:test-subject-id-1", urlMapping.getSubjectId().toString());
    }
}

/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
public class VersionHeaderExtractorTest extends AbstractIntegrationTest {

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private VersionHeaderExtractor versionHeaderExtractor;

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    @BeforeEach
    void setUp() {
        versionRepository.deleteAllAndFlush();
    }

    @Test
    void no_headers_without_versions_returns_default_schema() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/v1/versions");
        when(request.getHeader(anyString())).thenReturn(null);
        String versionSchemaFromHeader = versionHeaderExtractor.getVersionSchemaFromHeader(request);
        assertEquals(defaultSchema, versionSchemaFromHeader);
    }

    @Test
    void header_with_no_matching_version_returns_default_schema() {
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/v1/versions");
            when(request.getHeader(anyString())).thenReturn("abcd");
            when(request.getMethod()).thenReturn("POST");
            String versionSchemaFromHeader = versionHeaderExtractor.getVersionSchemaFromHeader(request);
            assertEquals(defaultSchema, versionSchemaFromHeader);
        }
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/v1/versions");
            when(request.getHeader(anyString())).thenReturn("abcd");
            when(request.getMethod()).thenReturn("GET");
            String versionSchemaFromHeader = versionHeaderExtractor.getVersionSchemaFromHeader(request);
            assertEquals(defaultSchema, versionSchemaFromHeader);
        }
    }

    @Test
    void header_with_matching_version_returns_correct_schema() {
        Version version = new Version();
        version.setVersionType(VersionType.BETA);
        Version saved = versionRepository.save(version);
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/v1/subjects");
            when(request.getHeader(anyString())).thenReturn(saved.getHash());
            when(request.getMethod()).thenReturn("POST");
            String versionSchemaFromHeader = versionHeaderExtractor.getVersionSchemaFromHeader(request);
            assertEquals(String.format("%s_%s", defaultSchema, saved.getHash()), versionSchemaFromHeader);
        }
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/v1/subjects");
            when(request.getHeader(anyString())).thenReturn(saved.getHash());
            when(request.getMethod()).thenReturn("GET");
            String versionSchemaFromHeader = versionHeaderExtractor.getVersionSchemaFromHeader(request);
            assertEquals(String.format("%s_%s", defaultSchema, saved.getHash()), versionSchemaFromHeader);
        }
    }

    @Test
    void no_header_returns_published_schema_for_GET_and_default_for_POST() {
        Version version = new Version();
        version.setVersionType(VersionType.PUBLISHED);
        Version saved = versionRepository.save(version);
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/v1/subjects");
            when(request.getHeader(anyString())).thenReturn(null);
            when(request.getMethod()).thenReturn("GET");
            String versionSchemaFromHeader = versionHeaderExtractor.getVersionSchemaFromHeader(request);
            assertEquals(String.format("%s_%s", defaultSchema, saved.getHash()), versionSchemaFromHeader);
        }
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/v1/subjects");
            when(request.getHeader(anyString())).thenReturn(null);
            when(request.getMethod()).thenReturn("POST");
            String versionSchemaFromHeader = versionHeaderExtractor.getVersionSchemaFromHeader(request);
            assertEquals(defaultSchema, versionSchemaFromHeader);
        }
    }
}

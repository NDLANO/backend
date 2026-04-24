/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Takes a http request and extracts a given header. Returns a database schema name.
 */
@Component
public class VersionHeaderExtractor {

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    private final VersionService versionService;

    private final VersionRepository versionRepository;

    public VersionHeaderExtractor(VersionRepository versionRepository, VersionService versionService) {
        this.versionRepository = versionRepository;
        this.versionService = versionService;
    }

    public String getVersionSchemaFromHeader(HttpServletRequest req) {
        String versionHash = req.getHeader("VersionHash");
        if (req.getRequestURI().startsWith("/v1/versions")) {
            return defaultSchema;
        }
        try {
            if (versionHash == null) {
                // No header, check published and use for gets
                Optional<Version> published = versionRepository.findFirstByVersionType(VersionType.PUBLISHED);
                if (published.isPresent() && "GET".equals(req.getMethod())) {
                    // Use published for all GETs
                    return versionService.schemaFromHash(published.get().getHash());
                }
            } else {
                // Header supplied, use that version if in database
                Optional<Version> version = versionRepository.findFirstByHash(versionHash);
                if (version.isPresent()) {
                    return versionService.schemaFromHash(version.get().getHash());
                }
            }
            // Either no header or no version matching header. Use default schema.
            return defaultSchema;
        } catch (Exception e) {
            // Something happened when fetching version!
            return defaultSchema;
        }
    }
}

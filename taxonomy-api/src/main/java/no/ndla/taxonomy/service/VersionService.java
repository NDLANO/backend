/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.rest.v1.commands.VersionPostPut;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.task.Deleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class VersionService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EntityManager entityManager;
    private final VersionRepository versionRepository;
    private final NodeConnectionService nodeConnectionService;
    private final URNValidator validator = new URNValidator();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    public VersionService(
            EntityManager entityManager,
            VersionRepository versionRepository,
            NodeConnectionService nodeConnectionService) {
        this.entityManager = entityManager;
        this.versionRepository = versionRepository;
        this.nodeConnectionService = nodeConnectionService;
    }

    @Transactional
    public void delete(URI publicId) {
        final var versionToDelete = versionRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Version was not found"));

        String schema = schemaFromHash(versionToDelete.getHash());
        try {
            entityManager
                    .createNativeQuery(String.format("DROP SCHEMA %s CASCADE", schema))
                    .executeUpdate();
        } catch (PersistenceException pe) {
            logger.warn("Failed to drop schema. Possible manual cleanup required");
        }
        versionRepository.delete(versionToDelete);
    }

    public List<VersionDTO> getVersions() {
        return versionRepository.findAll().stream().map(VersionDTO::new).collect(Collectors.toList());
    }

    public List<VersionDTO> getVersionsOfType(VersionType versionType) {
        return versionRepository.findByVersionType(versionType).stream()
                .map(VersionDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    @Async
    public void publishBetaAndArchiveCurrent(URI id) {
        Optional<Version> published = versionRepository.findFirstByVersionType(VersionType.PUBLISHED);
        if (published.isPresent()) {
            Version version = published.get();
            version.setVersionType(VersionType.ARCHIVED);
            version.setArchived(Instant.now());
            versionRepository.saveAndFlush(version);
        }
        Version beta = versionRepository.getByPublicId(id);
        beta.setVersionType(VersionType.PUBLISHED);
        beta.setLocked(true);
        beta.setPublished(Instant.now());
        versionRepository.saveAndFlush(beta);

        disconnectAllInvisibleNodes(beta.getHash());
    }

    private void disconnectAllInvisibleNodes(String hash) {
        // Use a task to run in a separate thread against a specified schema
        // Do not care about the result so no need to wait for it
        try {
            Deleter deleter = new Deleter();
            deleter.setNodeConnectionService(nodeConnectionService);
            deleter.setVersion(schemaFromHash(hash));
            executor.submit(deleter).get();
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }
    }

    @Transactional
    public Version createNewVersion(Optional<URI> sourceId, VersionPostPut command) {
        Version entity = new Version();
        command.getId().ifPresent(id -> {
            validator.validate(id, entity);
            entity.setPublicId(id);
        });
        command.apply(entity);
        Version version = versionRepository.save(entity);

        String sourceSchema = defaultSchema;
        if (sourceId.isPresent()) {
            Version source = versionRepository.getByPublicId(sourceId.get());
            if (source != null) {
                sourceSchema = schemaFromHash(source.getHash());
            }
        }

        String schema = schemaFromHash(version.getHash());
        // JPA does not like functions returning void so adds a count(*) to sql.
        entityManager
                .createNativeQuery(String.format(
                        "SELECT count(*) from clone_schema('%s', '%s', true, false)", sourceSchema, schema))
                .getSingleResult();
        return version;
    }

    public String schemaFromHash(String hash) {
        if (hash != null) return String.format("%s%s", defaultSchema, "_" + hash);
        return defaultSchema;
    }
}

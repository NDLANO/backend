/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.VersionPostPut;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.VersionService;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/versions", "/v1/versions/"})
public class Versions extends CrudController<Version> {
    private final VersionRepository versionRepository;
    private final VersionService versionService;

    public Versions(VersionService versionService, VersionRepository versionRepository) {
        super(versionRepository);
        this.versionService = versionService;
        this.versionRepository = versionRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all versions")
    @Transactional(readOnly = true)
    public List<VersionDTO> getAllVersions(
            @Parameter(description = "Version type", example = "PUBLISHED")
                    @RequestParam(value = "type", required = false, defaultValue = "")
                    Optional<VersionType> versionType,
            @Parameter(description = "Version hash", example = "ndla") @RequestParam(value = "hash", required = false)
                    Optional<String> hash) {
        if (versionType.isPresent()) return versionService.getVersionsOfType(versionType.get());
        return hash.map(s -> List.of(versionRepository
                        .findFirstByHash(s)
                        .map(VersionDTO::new)
                        .orElseThrow(() -> new NotFoundHttpResponseException("Version not found"))))
                .orElseGet(versionService::getVersions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single version")
    @Transactional(readOnly = true)
    public VersionDTO getVersion(@PathVariable("id") URI id) {
        return versionRepository
                .findFirstByPublicId(id)
                .map(VersionDTO::new)
                .orElseThrow(() -> new NotFoundHttpResponseException("Version not found"));
    }

    @PostMapping
    @Operation(
            summary = "Creates a new version",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public ResponseEntity<Void> createVersion(
            @Parameter(description = "Base new version on version with this id") @RequestParam(value = "sourceId")
                    Optional<URI> sourceId,
            @Parameter(name = "version", description = "The new version") @RequestBody VersionPostPut command) {
        // Don't call doPost because we need to create new schema
        Version version = versionService.createNewVersion(sourceId, command);
        URI location = URI.create(getLocation() + "/" + version.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Updates a version",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public void updateVersion(
            @PathVariable("id") URI id,
            @Parameter(name = "version", description = "The updated version.") @RequestBody VersionPostPut command) {
        updateEntity(id, command);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletes a version by publicId",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        Optional<Version> version = versionRepository.findFirstByPublicId(id);
        if (version.isEmpty() || version.get().isLocked()) {
            throw new InvalidArgumentServiceException("Cannot delete locked version");
        }
        versionService.delete(id);
    }

    @PutMapping("/{id}/publish")
    @Operation(
            summary = "Publishes a version",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public void publishVersion(@PathVariable("id") URI id) {
        Optional<Version> version = versionRepository.findFirstByPublicId(id);
        if (version.isEmpty() || version.get().getVersionType() != VersionType.BETA) {
            throw new InvalidArgumentServiceException("Version has wrong type");
        }
        versionService.publishBetaAndArchiveCurrent(id);
    }
}

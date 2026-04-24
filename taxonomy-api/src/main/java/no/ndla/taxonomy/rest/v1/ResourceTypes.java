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
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypeDTO;
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypePUT;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/resource-types", "/v1/resource-types/"})
public class ResourceTypes extends CrudController<ResourceType> {

    private final ResourceTypeRepository resourceTypeRepository;

    public ResourceTypes(ResourceTypeRepository resourceTypeRepository) {
        super(resourceTypeRepository);

        this.resourceTypeRepository = resourceTypeRepository;
    }

    @GetMapping
    @Operation(summary = "Gets a list of all resource types")
    @Transactional(readOnly = true)
    public List<ResourceTypeDTO> getAllResourceTypes(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        // Returns all resource types that is NOT a subtype
        return resourceTypeRepository.findAllByParentIncludingTranslationsAndFirstLevelSubtypes(null).stream()
                .map(resourceType -> new ResourceTypeDTO(resourceType, language, 100))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single resource type")
    @Transactional(readOnly = true)
    public ResourceTypeDTO getResourceType(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return resourceTypeRepository
                .findFirstByPublicIdIncludingTranslations(id)
                .map(resourceType -> new ResourceTypeDTO(resourceType, language, 0))
                .orElseThrow(() -> new NotFoundException("ResourceType", id));
    }

    @PostMapping
    @Operation(
            summary = "Adds a new resource type",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createResourceType(
            @Parameter(name = "resourceType", description = "The new resource type") @RequestBody
                    ResourceTypePUT command) {
        ResourceType resourceType = new ResourceType();
        if (null != command.parentId) {
            ResourceType parent = resourceTypeRepository.getByPublicId(command.parentId);
            resourceType.setParent(parent);
        }
        if (command.order > -1) {
            resourceType.setOrder(command.order);
        }
        return createEntity(resourceType, command);
    }

    @PutMapping("/{id}")
    @Operation(
            summary =
                    "Updates a resource type. Use to update which resource type is parent. You can also update the id, take care!",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateResourceType(
            @PathVariable URI id,
            @Parameter(
                            name = "resourceType",
                            description = "The updated resource type. Fields not included will be set to null.")
                    @RequestBody
                    ResourceTypePUT command) {
        ResourceType resourceType = updateEntity(id, command);

        ResourceType parent = null;
        if (command.parentId != null) {
            parent = resourceTypeRepository.getByPublicId(command.parentId);
        }
        resourceType.setParent(parent);
        if (command.id != null) {
            resourceType.setPublicId(command.id);
        }
        if (command.order > -1) {
            resourceType.setOrder(command.order);
        }
    }

    @GetMapping("/{id}/subtypes")
    @Operation(summary = "Gets subtypes of one resource type")
    @Transactional(readOnly = true)
    public List<ResourceTypeDTO> getResourceTypeSubtypes(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    @Parameter(description = "If true, sub resource types are fetched recursively")
                    boolean recursive) {
        return resourceTypeRepository.findAllByParentPublicIdIncludingTranslationsAndFirstLevelSubtypes(id).stream()
                .map(resourceType -> new ResourceTypeDTO(resourceType, language, 100))
                .collect(Collectors.toList());
    }
}

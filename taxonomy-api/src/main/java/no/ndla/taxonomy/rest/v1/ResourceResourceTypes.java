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
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypeDTO;
import no.ndla.taxonomy.rest.v1.dtos.ResourceResourceTypePOST;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/resource-resourcetypes", "/v1/resource-resourcetypes/"})
public class ResourceResourceTypes {

    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final NodeService nodeService;

    public ResourceResourceTypes(
            ResourceResourceTypeRepository resourceResourceTypeRepository, NodeService nodeService) {
        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.nodeService = nodeService;
    }

    @PostMapping
    @Operation(
            summary = "Adds a resource type to a resource",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createResourceResourceType(
            @Parameter(name = "connection", description = "The new resource/resource type connection") @RequestBody
                    ResourceResourceTypePOST command) {
        ResourceResourceType resourceResourceType =
                nodeService.connectNodeResourceType(command.resourceId, command.resourceTypeId);

        URI location = URI.create("/resource-resourcetypes/" + resourceResourceType.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping({"/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Removes a resource type from a resource",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteResourceResourceType(@PathVariable("id") URI id) {
        resourceResourceTypeRepository.delete(resourceResourceTypeRepository.getByPublicId(id));
        resourceResourceTypeRepository.flush();
    }

    @GetMapping
    @Operation(summary = "Gets all connections between resources and resource types")
    @Transactional(readOnly = true)
    public List<ResourceResourceTypeDTO> getAllResourceResourceTypes() {
        return resourceResourceTypeRepository.findAllIncludingResourceAndResourceType().stream()
                .map(ResourceResourceTypeDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping({"/{id}"})
    @Operation(summary = "Gets a single connection between resource and resource type")
    @Transactional(readOnly = true)
    public ResourceResourceTypeDTO getResourceResourceType(@PathVariable("id") URI id) {
        ResourceResourceType result = resourceResourceTypeRepository.getByPublicId(id);
        return new ResourceResourceTypeDTO(result);
    }
}

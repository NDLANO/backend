/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.commands.ResourcePostPut;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.QualityEvaluationService;
import no.ndla.taxonomy.service.ResourceTypeService;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.NodeWithParents;
import no.ndla.taxonomy.service.dtos.ResourceTypeWithConnectionDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/resources", "/v1/resources/"})
public class Resources extends CrudControllerWithMetadata<Node> {
    private final Nodes nodes;
    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final NodeService nodeService;

    public Resources(
            Nodes nodes,
            NodeRepository nodeRepository,
            ResourceResourceTypeRepository resourceResourceTypeRepository,
            ContextUpdaterService contextUpdaterService,
            NodeService nodeService,
            QualityEvaluationService qualityEvaluationService,
            ResourceTypeService resourceTypeService) {
        super(nodeRepository, contextUpdaterService, nodeService, qualityEvaluationService, resourceTypeService);

        this.nodes = nodes;
        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.repository = nodeRepository;
        this.nodeService = nodeService;
    }

    @Override
    protected String getLocation() {
        return "/v1/resources";
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Lists all resources")
    @Transactional(readOnly = true)
    public List<NodeDTO> getAllResources(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "Filter by contentUri") @RequestParam(value = "contentURI", required = false)
                    Optional<URI> contentUri,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false)
                    Optional<String> key,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "value", required = false)
                    Optional<String> value,
            @Parameter(description = "Filter contexts by visibility")
                    @RequestParam(value = "isVisible", required = false)
                    Optional<Boolean> isVisible) {
        return nodes.getAllNodes(
                Optional.of(List.of(NodeType.RESOURCE)),
                language,
                contentUri,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                key,
                value,
                Optional.empty(),
                isVisible,
                true,
                true,
                Optional.empty(),
                Optional.empty());
    }

    @Deprecated
    @GetMapping("/search")
    @Operation(summary = "Search all resources")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> searchResources(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "How many results to return per page")
                    @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize,
            @Parameter(description = "Which page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Query to search names") @RequestParam(value = "query", required = false)
                    Optional<String> query,
            @Parameter(description = "Ids to fetch for query") @RequestParam(value = "ids", required = false)
                    Optional<List<String>> ids,
            @Parameter(description = "ContentURIs to fetch for query")
                    @RequestParam(value = "contentUris", required = false)
                    Optional<List<String>> contentUris) {
        return nodes.searchNodes(
                language,
                pageSize,
                page,
                query,
                ids,
                contentUris,
                Optional.of(List.of(NodeType.RESOURCE)),
                true,
                true,
                Optional.empty(),
                Optional.empty());
    }

    @Deprecated
    @GetMapping("/page")
    @Operation(summary = "Gets all resources paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> getResourcePage(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "The page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Size of page to fetch") @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize) {
        return nodes.getNodePage(language, page, pageSize, Optional.of(NodeType.RESOURCE), true, true, true);
    }

    @Deprecated
    @GetMapping("{id}")
    @Operation(summary = "Gets a single resource")
    @Transactional(readOnly = true)
    public NodeDTO getResource(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language) {
        return nodes.getNode(id, Optional.empty(), Optional.empty(), true, true, true, language);
    }

    @Deprecated
    @PutMapping("{id}")
    @Operation(
            summary = "Updates a resource",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateResource(
            @PathVariable("id") URI id,
            @Parameter(
                            name = "resource",
                            description = "the updated resource. Fields not included will be set to null.")
                    @RequestBody
                    @Schema(name = "ResourcePOST")
                    ResourcePostPut command) {
        nodes.updateEntity(id, command);
    }

    @Deprecated
    @PostMapping
    @Operation(
            summary = "Adds a new resource",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createResource(
            @Parameter(name = "resource", description = "the new resource") @RequestBody @Schema(name = "ResourcePUT")
                    ResourcePostPut command) {
        return nodes.createEntity(new Node(NodeType.RESOURCE), command);
    }

    @Deprecated
    @PostMapping("{id}/clone")
    @Operation(
            summary = "Clones a resource, including resource-types and translations",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> cloneResource(
            @Parameter(description = "Id of resource to clone", example = "urn:resource:1") @PathVariable("id")
                    URI publicId,
            @Parameter(name = "resource", description = "Object containing contentUri. Other values are ignored.")
                    @RequestBody
                    @Schema(name = "ResourcePOST")
                    ResourcePostPut command) {
        var entity = nodeService.cloneNode(publicId, Optional.ofNullable(command.contentUri));
        URI location = URI.create(getLocation() + "/" + entity.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @Deprecated
    @GetMapping("{id}/resource-types")
    @Operation(summary = "Gets all resource types associated with this resource")
    @Transactional(readOnly = true)
    public List<ResourceTypeWithConnectionDTO> getResourceTypes(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language) {

        return resourceResourceTypeRepository.resourceResourceTypeByParentId(id).stream()
                .map(resourceResourceType -> new ResourceTypeWithConnectionDTO(resourceResourceType, language))
                .toList();
    }

    @Deprecated
    @GetMapping("{id}/full")
    @Operation(summary = "Gets all parent topics, all filters and resourceTypes for this resource")
    @Transactional(readOnly = true)
    public NodeWithParents getResourceFull(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language) {
        return nodes.getNodeFull(id, language, true);
    }

    @Deprecated
    @DeleteMapping("{id}")
    @Operation(
            summary = "Deletes a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        nodes.deleteEntity(id);
    }
}

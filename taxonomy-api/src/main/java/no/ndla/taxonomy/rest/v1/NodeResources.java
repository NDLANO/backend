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
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourceDTO;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourcePOST;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourcePUT;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/node-resources", "/v1/node-resources/"})
public class NodeResources extends CrudControllerWithMetadata<NodeConnection> {

    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeConnectionService connectionService;

    public NodeResources(
            NodeRepository nodeRepository,
            NodeConnectionService connectionService,
            NodeConnectionRepository nodeConnectionRepository,
            ContextUpdaterService contextUpdaterService,
            NodeService nodeService,
            QualityEvaluationService qualityEvaluationService,
            ResourceTypeService resourceTypeService) {
        super(
                nodeConnectionRepository,
                contextUpdaterService,
                nodeService,
                qualityEvaluationService,
                resourceTypeService);
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeRepository = nodeRepository;
        this.connectionService = connectionService;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between node and resources")
    @Transactional(readOnly = true)
    public List<NodeResourceDTO> getAllNodeResources() {
        return nodeConnectionRepository.findAllByChildNodeType(NodeType.RESOURCE).stream()
                .map(NodeResourceDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and resources paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeResourceDTO> getNodeResourcesPage(
            @Parameter(description = "The page to fetch", required = true)
                    @RequestParam(value = "page", defaultValue = "1")
                    int page,
            @Parameter(description = "Size of page to fetch", required = true)
                    @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize) {

        if (page < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page - 1, pageSize);
        var connections = nodeConnectionRepository.findIdsPaginatedByChildNodeType(pageRequest, NodeType.RESOURCE);
        var ids = connections.stream().map(DomainEntity::getId).toList();
        var results = nodeConnectionRepository.findByIds(ids);
        var contents = results.stream().map(NodeResourceDTO::new).collect(Collectors.toList());
        return new SearchResultDTO<>(connections.getTotalElements(), page, pageSize, contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a specific connection between a node and a resource")
    @Transactional(readOnly = true)
    public NodeResourceDTO getNodeResource(@PathVariable("id") URI id) {
        NodeConnection connection = nodeConnectionRepository.getByPublicId(id);
        return new NodeResourceDTO(connection);
    }

    @PostMapping
    @Operation(
            summary = "Adds a resource to a node",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Created201ApiResponse
    @Transactional
    public ResponseEntity<Void> createNodeResource(
            @Parameter(name = "connection", description = "new node/resource connection ") @RequestBody
                    NodeResourcePOST command) {
        var parent = nodeRepository.getByPublicId(command.nodeId);
        var child = nodeRepository.getByPublicId(command.resourceId);
        var relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")));
        var rank = command.rank.orElse(null);

        final var nodeConnection = connectionService.connectParentChild(
                parent, child, relevance, rank, command.primary, NodeConnectionType.BRANCH);

        var location = URI.create("/node-resources/" + nodeConnection.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Removes a resource from a node",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        var connection = nodeConnectionRepository.getByPublicId(id);
        connectionService.disconnectParentChildConnection(connection);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Updates a connection between a node and a resource",
            description = "Use to update which node is primary to the resource or to change sorting order.",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateNodeResource(
            @PathVariable("id") URI id,
            @Parameter(name = "connection", description = "Updated node/resource connection") @RequestBody
                    NodeResourcePUT command) {
        final var nodeResource = nodeConnectionRepository.getByPublicId(id);
        var relevance = Relevance.unsafeGetRelevance(command.relevanceId.orElse(URI.create("urn:relevance:core")));
        if (nodeResource.isPrimary().orElse(false) && !command.primary.orElse(false)) {
            throw new PrimaryParentRequiredException();
        }

        connectionService.updateParentChild(nodeResource, relevance, command.rank, command.primary);
    }
}

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
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.TopicPostPut;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.QualityEvaluationService;
import no.ndla.taxonomy.service.ResourceTypeService;
import no.ndla.taxonomy.service.dtos.ConnectionDTO;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/topics", "/v1/topics/"})
public class Topics extends CrudControllerWithMetadata<Node> {
    private final Nodes nodes;

    public Topics(
            Nodes nodes,
            NodeRepository nodeRepository,
            NodeService nodeService,
            ContextUpdaterService contextUpdaterService,
            QualityEvaluationService qualityEvaluationService,
            ResourceTypeService resourceTypeService) {
        super(nodeRepository, contextUpdaterService, nodeService, qualityEvaluationService, resourceTypeService);

        this.nodes = nodes;
        this.nodeService = nodeService;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all topics")
    @Transactional(readOnly = true)
    public List<NodeDTO> getAllTopics(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
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
                Optional.of(List.of(NodeType.TOPIC)),
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
    @Operation(summary = "Search all topics")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> searchTopics(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
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
                Optional.of(List.of(NodeType.TOPIC)),
                true,
                true,
                Optional.empty(),
                Optional.empty());
    }

    @Deprecated
    @GetMapping("/page")
    @Operation(summary = "Gets all topics paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> getTopicsPage(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "The page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Size of page to fetch") @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize) {
        return nodes.getNodePage(language, page, pageSize, Optional.of(NodeType.TOPIC), true, true, true);
    }

    @Deprecated
    @GetMapping("/{id}")
    @Operation(summary = "Gets a single topic")
    @Transactional(readOnly = true)
    public NodeDTO getTopic(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language) {
        return nodes.getNode(id, Optional.empty(), Optional.empty(), true, true, true, language);
    }

    @Deprecated
    @PostMapping
    @Operation(
            summary = "Creates a new topic",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createTopic(
            @Parameter(name = "connection", description = "The new topic") @RequestBody @Schema(name = "TopicPOST")
                    TopicPostPut command) {
        return nodes.createEntity(new Node(NodeType.TOPIC), command);
    }

    @Deprecated
    @PutMapping("/{id}")
    @Operation(
            summary = "Updates a single topic",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateTopic(
            @PathVariable("id") URI id,
            @Parameter(name = "topic", description = "The updated topic. Fields not included will be set to null.")
                    @RequestBody
                    TopicPostPut command) {
        nodes.updateEntity(id, command);
    }

    @Deprecated
    @GetMapping("/{id}/topics")
    @Operation(summary = "Gets all subtopics for this topic")
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getTopicSubTopics(
            @Parameter(name = "id", required = true) @PathVariable("id") URI id,
            @Parameter(
                            description =
                                    "Select filters by subject id if filter list is empty. Used as alternative to specify filters.")
                    @RequestParam(value = "subject", required = false, defaultValue = "")
                    URI subjectId,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language) {
        return nodes.getChildren(
                id,
                Optional.of(List.of(NodeType.TOPIC)),
                List.of(NodeConnectionType.BRANCH),
                false,
                language,
                true,
                true,
                true);
    }

    @Deprecated
    @GetMapping("/{id}/connections")
    @Operation(summary = "Gets all subjects and subtopics this topic is connected to")
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getAllTopicConnections(@PathVariable("id") URI id) {
        return nodes.getAllConnections(id);
    }

    @Deprecated
    @DeleteMapping("/{id}")
    @Operation(
            description = "Deletes a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        nodes.deleteEntity(id);
    }

    @Deprecated
    @GetMapping("/{id}/resources")
    @Operation(
            summary = "Gets all resources for the given topic",
            tags = {"topics"})
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getTopicResources(
            @Parameter(name = "id", required = true) @PathVariable("id") URI topicId,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language,
            @Parameter(description = "If true, resources from subtopics are fetched recursively")
                    @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    boolean recursive,
            @Parameter(
                            description =
                                    "Select by resource type id(s). If not specified, resources of all types will be returned."
                                            + "Multiple ids may be separated with comma or the parameter may be repeated for each id.")
                    @RequestParam(value = "type", required = false)
                    Optional<List<URI>> resourceTypeIds,
            @Parameter(description = "Select by relevance. If not specified, all resources will be returned.")
                    @RequestParam(value = "relevance", required = false)
                    Optional<URI> relevance) {
        return nodes.getResources(topicId, language, true, true, true, recursive, resourceTypeIds, relevance);
    }

    @Deprecated
    @PutMapping("/{id}/makeResourcesPrimary")
    @Operation(
            summary = "Makes all connected resources primary",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public ResponseEntity<Boolean> makeResourcesPrimary(
            @Parameter(name = "id", required = true) @PathVariable("id") URI nodeId,
            @Parameter(description = "If true, children are fetched recursively")
                    @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    boolean recursive) {
        return nodes.makeResourcesPrimary(nodeId, recursive);
    }
}

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
import no.ndla.taxonomy.rest.v1.commands.SubjectPostPut;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.QualityEvaluationService;
import no.ndla.taxonomy.service.ResourceTypeService;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/subjects", "/v1/subjects/"})
public class Subjects extends CrudControllerWithMetadata<Node> {
    private final Nodes nodes;

    public Subjects(
            Nodes nodes,
            ContextUpdaterService contextUpdaterService,
            NodeService nodeService,
            NodeRepository nodeRepository,
            QualityEvaluationService qualityEvaluationService,
            ResourceTypeService resourceTypeService) {
        super(nodeRepository, contextUpdaterService, nodeService, qualityEvaluationService, resourceTypeService);

        this.nodes = nodes;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all subjects")
    @Transactional(readOnly = true)
    public List<NodeDTO> getAllSubjects(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false)
                    Optional<String> key,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "value", required = false)
                    Optional<String> value,
            @Parameter(description = "Filter contexts by visibility")
                    @RequestParam(value = "isVisible", required = false)
                    Optional<Boolean> isVisible) {
        return nodes.getAllNodes(
                Optional.of(List.of(NodeType.SUBJECT)),
                language,
                Optional.empty(),
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
    @Operation(summary = "Search all subjects")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> searchSubjects(
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
                Optional.of(List.of(NodeType.SUBJECT)),
                true,
                true,
                Optional.empty(),
                Optional.empty());
    }

    @Deprecated
    @GetMapping("/page")
    @Operation(summary = "Gets all nodes paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> getSubjectPage(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "The page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Size of page to fetch") @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize) {
        return nodes.getNodePage(language, page, pageSize, Optional.of(NodeType.SUBJECT), true, true, true);
    }

    @Deprecated
    @GetMapping("/{id}")
    @Operation(
            summary = "Gets a single subject",
            description = "Default language will be returned if desired language not found or if parameter is omitted.")
    @Transactional(readOnly = true)
    public NodeDTO getSubject(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language) {
        return nodes.getNode(id, Optional.empty(), Optional.empty(), true, true, true, language);
    }

    @Deprecated
    @PutMapping("/{id}")
    @Operation(
            summary = "Updates a subject",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateSubject(
            @PathVariable("id") URI id,
            @Parameter(name = "subject", description = "The updated subject. Fields not included will be set to null.")
                    @RequestBody
                    @Schema(name = "SubjectPOST")
                    SubjectPostPut command) {
        nodes.updateEntity(id, command);
    }

    @Deprecated
    @PostMapping
    @Operation(
            summary = "Creates a new subject",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createSubject(
            @Parameter(name = "subject", description = "The new subject") @RequestBody @Schema(name = "SubjectPUT")
                    SubjectPostPut command) {
        final var subject = new Node(NodeType.SUBJECT);
        return nodes.createEntity(subject, command);
    }

    @Deprecated
    @GetMapping("/{id}/topics")
    @Operation(
            summary = "Gets all children associated with a subject",
            description =
                    "This resource is read-only. To update the relationship between nodes, use the resource /subject-topics.")
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getSubjectChildren(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language,
            @Parameter(description = "If true, subtopics are fetched recursively")
                    @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    boolean recursive,
            @Parameter(description = "Select by relevance. If not specified, all nodes will be returned.")
                    @RequestParam(value = "relevance", required = false)
                    Optional<URI> relevance) {
        var children = nodes.getChildren(
                id,
                Optional.of(List.of(NodeType.TOPIC)),
                List.of(NodeConnectionType.BRANCH),
                recursive,
                language,
                true,
                true,
                true);
        return relevance
                .map(rel -> children.stream()
                        .filter(node -> node.getRelevanceId().isPresent()
                                && node.getRelevanceId().get().equals(rel))
                        .toList())
                .orElse(children);
    }

    @Deprecated
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletes a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        nodes.deleteEntity(id);
    }

    @Deprecated
    @GetMapping("/{subjectId}/resources")
    @Operation(
            summary =
                    "Gets all resources for a subject. Searches recursively in all children of this node."
                            + "The ordering of resources will be based on the rank of resources relative to the node they belong to.",
            tags = {"subjects"})
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getSubjectResources(
            @PathVariable("subjectId") URI subjectId,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language,
            @Parameter(
                            description =
                                    "Filter by resource type id(s). If not specified, resources of all types will be returned."
                                            + "Multiple ids may be separated with comma or the parameter may be repeated for each id.")
                    @RequestParam(value = "type", required = false)
                    Optional<List<URI>> resourceTypeIds,
            @Parameter(description = "Select by relevance. If not specified, all resources will be returned.")
                    @RequestParam(value = "relevance", required = false)
                    Optional<URI> relevance) {
        return nodes.getResources(subjectId, language, true, true, true, true, resourceTypeIds, relevance);
    }
}

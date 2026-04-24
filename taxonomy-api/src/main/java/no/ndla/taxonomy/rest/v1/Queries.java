/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyContextDTO;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/queries"})
public class Queries {
    private final Topics topicController;
    private final Resources resourceController;
    private final NodeService nodeService;

    public Queries(Topics topicController, Resources resourceController, NodeService nodeService) {
        this.topicController = topicController;
        this.resourceController = resourceController;
        this.nodeService = nodeService;
    }

    @GetMapping("/{contentURI}")
    @Operation(summary = "Gets a list of contexts matching given contentURI, empty list if no matches are found.")
    @Transactional(readOnly = true)
    public List<TaxonomyContextDTO> contextByContentURI(
            @PathVariable("contentURI") Optional<URI> contentURI,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "Whether to filter out contexts if a parent (or the node itself) is non-visible")
                    @RequestParam(value = "filterVisibles", required = false, defaultValue = "true")
                    boolean filterVisibles) {
        return nodeService.getSearchableByContentUri(contentURI, filterVisibles, language);
    }

    @GetMapping("/contextId")
    @Operation(summary = "Gets a list of contexts matching given contextId, empty list if no matches are found.")
    @Transactional(readOnly = true)
    public List<TaxonomyContextDTO> contextByContextId(
            @RequestParam("contextId") Optional<String> contextId,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language) {
        return nodeService.getContextByContextId(contextId, language);
    }

    @GetMapping("/path")
    @Operation(
            summary =
                    "Gets a list of contexts matching given pretty url with contextId, empty list if no matches are found.")
    @Transactional(readOnly = true)
    public List<TaxonomyContextDTO> queryPath(
            @RequestParam("path") Optional<String> path,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language) {
        return nodeService.getContextByPath(path, language);
    }

    @GetMapping("/resources")
    @Operation(
            summary =
                    "Gets a list of resources matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/resources?contentURI= instead")
    @Transactional(readOnly = true)
    @Deprecated
    public List<NodeDTO> queryResources(
            @RequestParam("contentURI") Optional<URI> contentURI,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false)
                    Optional<String> key,
            @Parameter(description = "Fitler by key and value") @RequestParam(value = "value", required = false)
                    Optional<String> value,
            @Parameter(description = "Filter by visible") @RequestParam(value = "isVisible", required = false)
                    Optional<Boolean> isVisible) {
        return resourceController.getAllResources(language, contentURI, key, value, isVisible);
    }

    @GetMapping("/topics")
    @Operation(
            summary =
                    "Gets a list of topics matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/topics?contentURI= instead")
    @Transactional(readOnly = true)
    @Deprecated
    public List<NodeDTO> queryTopics(
            @RequestParam("contentURI") URI contentURI,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false)
                    Optional<String> key,
            @Parameter(description = "Fitler by key and value") @RequestParam(value = "value", required = false)
                    Optional<String> value,
            @Parameter(description = "Filter by visible") @RequestParam(value = "isVisible", required = false)
                    Optional<Boolean> isVisible) {
        return topicController.getAllTopics(language, Optional.of(contentURI), key, value, isVisible);
    }
}

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.dtos.ContextDTO;
import no.ndla.taxonomy.rest.v1.dtos.ContextPOST;
import no.ndla.taxonomy.rest.v1.responses.Created201ApiResponse;
import no.ndla.taxonomy.service.ContextUpdaterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/contexts", "/v1/contexts/"})
@Transactional(readOnly = true)
public class Contexts {
    private final NodeRepository nodeRepository;
    private final ContextUpdaterService contextUpdaterService;

    public Contexts(NodeRepository nodeRepository, ContextUpdaterService contextUpdaterService) {
        this.nodeRepository = nodeRepository;
        this.contextUpdaterService = contextUpdaterService;
    }

    @GetMapping
    @Operation(summary = "Gets id of all nodes registered as context")
    public List<ContextDTO> getAllContexts(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    String language) {

        final var nodes = nodeRepository.findAllByContextIncludingCachedUrlsAndTranslations(true);

        final var contextDocuments = new ArrayList<>(nodes.stream()
                .map(node -> new ContextDTO(
                        node.getPublicId(),
                        node.getTranslatedName(language),
                        node.getPrimaryPath().orElse(null)))
                .toList());

        contextDocuments.sort(Comparator.comparing(ContextDTO::getId));

        return contextDocuments;
    }

    @PostMapping
    @Operation(
            summary = "Registers a new node as context",
            description =
                    "All subjects are already contexts and may not be added again. The node to register as context must exist already.",
            security = {@SecurityRequirement(name = "oauth")})
    @Created201ApiResponse
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createContext(
            @Parameter(
                            name = "context",
                            description = "object containing public id of the node to be registered as context")
                    @RequestBody
                    ContextPOST command) {
        Node node = nodeRepository.getByPublicId(command.id);
        node.setContext(true);
        URI location = URI.create("/v1/contexts/" + node.getPublicId());

        contextUpdaterService.updateContexts(node);

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Removes context registration from node",
            description = "Does not remove the underlying node, only marks it as not being a context",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteContext(@PathVariable("id") URI id) {
        Node node = nodeRepository.getByPublicId(id);
        node.setContext(false);

        contextUpdaterService.updateContexts(node);
    }
}

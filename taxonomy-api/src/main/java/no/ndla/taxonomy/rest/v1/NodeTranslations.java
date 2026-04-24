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
import java.util.List;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/nodes/{id}/translations", "/v1/nodes/{id}/translations/"})
public class NodeTranslations {

    private final NodeRepository nodeRepository;

    public NodeTranslations(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all translations for a single node")
    @Transactional(readOnly = true)
    public List<TranslationDTO> getAllNodeTranslations(@PathVariable("id") URI id) {
        Node node = nodeRepository.getByPublicId(id);
        List<TranslationDTO> result = new ArrayList<>();
        node.getTranslations().forEach(t -> result.add(new TranslationDTO(t)));
        return result;
    }

    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single node")
    @Transactional(readOnly = true)
    public TranslationDTO getNodeTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Node node = nodeRepository.getByPublicId(id);
        var translation = node.getTranslation(language)
                .orElseThrow(
                        () -> new NotFoundException("translation with language code " + language + " for node", id));
        return new TranslationDTO(translation);
    }

    @PutMapping("/{language}")
    @Operation(
            summary = "Creates or updates a translation of a node",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void createUpdateNodeTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language,
            @Parameter(name = "command", description = "The new or updated translation") @RequestBody
                    TranslationPUT command) {
        Node node = nodeRepository.getByPublicId(id);
        node.addTranslation(command.name, language);
        nodeRepository.save(node);
    }

    @DeleteMapping("/{language}")
    @Operation(
            summary = "Deletes a translation",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteNodeTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Node node = nodeRepository.getByPublicId(id);
        node.getTranslation(language).ifPresent(translation -> {
            node.removeTranslation(language);
            nodeRepository.save(node);
        });
    }
}

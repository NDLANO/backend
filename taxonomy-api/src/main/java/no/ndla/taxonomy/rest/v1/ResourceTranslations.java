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
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/resources/{id}/translations", "/v1/resources/{id}/translations/"})
public class ResourceTranslations {

    private final NodeTranslations nodeTranslations;

    public ResourceTranslations(NodeTranslations nodeTranslations) {
        this.nodeTranslations = nodeTranslations;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single resource")
    @Transactional(readOnly = true)
    public List<TranslationDTO> getAllResourceTranslations(@PathVariable("id") URI id) {
        return nodeTranslations.getAllNodeTranslations(id);
    }

    @Deprecated
    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single resource")
    @Transactional(readOnly = true)
    public TranslationDTO getResourceTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        return nodeTranslations.getNodeTranslation(id, language);
    }

    @Deprecated
    @PutMapping("/{language}")
    @Operation(
            summary = "Creates or updates a translation of a resource",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void createUpdateResourceTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language,
            @Parameter(name = "resource", description = "The new or updated translation") @RequestBody
                    TranslationPUT command) {
        nodeTranslations.createUpdateNodeTranslation(id, language, command);
    }

    @Deprecated
    @DeleteMapping("/{language}")
    @Operation(
            summary = "Deletes a translation",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteResourceTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        nodeTranslations.deleteNodeTranslation(id, language);
    }
}

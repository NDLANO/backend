/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.net.URI;
import java.util.List;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/relevances/{id}/translations", "/v1/relevances/{id}/translations/"})
public class RelevanceTranslations {
    Relevances relevances;

    public RelevanceTranslations(Relevances relevances) {
        this.relevances = relevances;
    }

    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single relevance")
    @Transactional(readOnly = true)
    public List<TranslationDTO> getAllRelevanceTranslations(@PathVariable("id") URI id) {
        var relevance = relevances.getRelevance(id, "");
        return relevance.getTranslations().stream().toList();
    }

    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single relevance")
    @Transactional(readOnly = true)
    public TranslationDTO getRelevanceTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        var relevance = relevances.getRelevance(id, language);
        return relevance
                .getTranslation(language)
                .orElseThrow(() ->
                        new NotFoundException("translation with language code " + language + " for relevance", id));
    }
}

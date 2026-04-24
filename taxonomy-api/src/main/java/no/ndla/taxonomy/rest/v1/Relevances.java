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
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.rest.v1.dtos.RelevanceDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/relevances", "/v1/relevances/"})
public class Relevances {
    public Relevances() {}

    @GetMapping
    @Operation(summary = "Gets all relevances")
    @Transactional(readOnly = true)
    public List<RelevanceDTO> getAllRelevances(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return Relevance.getRelevances().stream()
                .map(RelevanceDTO::new)
                .map(dto -> dto.getTranslated(language))
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Gets a single relevance",
            description = "Default language will be returned if desired language not found or if parameter is omitted.")
    @Transactional(readOnly = true)
    public RelevanceDTO getRelevance(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return Relevance.getRelevance(id)
                .map(RelevanceDTO::new)
                .map(dto -> dto.getTranslated(language))
                .orElseThrow(() -> new NotFoundException("Relevance", id));
    }
}

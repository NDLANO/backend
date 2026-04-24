/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.QualityEvaluationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/admin"})
public class Admin {
    private final NodeService nodeService;
    private final QualityEvaluationService qualityEvaluationService;

    public Admin(NodeService nodeService, QualityEvaluationService qualityEvaluationService) {
        this.nodeService = nodeService;
        this.qualityEvaluationService = qualityEvaluationService;
    }

    @GetMapping("/buildContexts")
    @Operation(
            summary = "Updates contexts for all roots. Requires taxonomy:admin access.",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public void buildAllContexts() {
        nodeService.buildAllContextsAsync();
    }

    @PostMapping("/buildAverageTree/{id}")
    @Operation(
            summary = "Updates average tree for the provided node. Requires taxonomy:admin access.",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public void buildAverageTree(@PathVariable("id") URI id) {
        qualityEvaluationService.updateEntireAverageTreeForNode(id);
    }

    @PostMapping("/buildAverageTree")
    @Operation(
            summary = "Updates average tree for all nodes. Requires taxonomy:admin access.",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public void buildAverageTree() {
        qualityEvaluationService.updateQualityEvaluationOfAllNodes();
    }
}

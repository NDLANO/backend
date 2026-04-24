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
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.dtos.ResolvedOldUrl;
import no.ndla.taxonomy.rest.v1.dtos.UrlMapping;
import no.ndla.taxonomy.service.UrlResolverService;
import no.ndla.taxonomy.service.dtos.ResolvedUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/url")
public class UrlResolver {
    private final UrlResolverService urlResolverService;

    @Autowired
    public UrlResolver(UrlResolverService urlResolverService) {
        this.urlResolverService = urlResolverService;
    }

    @GetMapping("/resolve")
    @Transactional(readOnly = true)
    public ResolvedUrl resolve(
            @RequestParam String path,
            @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    String language) {
        return urlResolverService
                .resolveUrl(path, language)
                .orElseThrow(() -> new NotFoundHttpResponseException("Element with path was not found"));
    }

    @GetMapping("/mapping")
    @Operation(summary = "Returns path for an url or HTTP 404")
    @Transactional(readOnly = true)
    public ResolvedOldUrl getTaxonomyPathForUrl(
            @Parameter(description = "url in old rig except 'https://'", example = "ndla.no/nb/node/142542?fag=52253")
                    @RequestParam
                    String url) {
        ResolvedOldUrl resolvedOldUrl = new ResolvedOldUrl();
        resolvedOldUrl.path = urlResolverService.resolveOldUrl(url).orElseThrow(() -> new NotFoundException(url));
        return resolvedOldUrl;
    }

    @PutMapping("/mapping")
    @Operation(
            summary = "Inserts or updates a mapping from url to nodeId and optionally subjectId",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void putTaxonomyNodeAndSubjectForOldUrl(@RequestBody UrlMapping urlMapping) {
        try {
            urlResolverService.putUrlMapping(
                    urlMapping.url, URI.create(urlMapping.nodeId), URI.create(urlMapping.subjectId));
        } catch (UrlResolverService.NodeIdNotFoundExeption ex) {
            throw new NotFoundHttpResponseException(ex.getMessage());
        }
    }
}

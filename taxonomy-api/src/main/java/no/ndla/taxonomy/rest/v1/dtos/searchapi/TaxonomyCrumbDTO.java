/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.searchapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

@Schema(
        name = "TaxonomyCrumb",
        requiredProperties = {"id", "contextId", "name", "path", "url"})
public record TaxonomyCrumbDTO(
        @JsonProperty @Schema(description = "The publicId of the node")
        URI id,

        @JsonProperty @Schema(description = "Unique id of context based on root + parent connection")
        String contextId,

        @JsonProperty @Schema(description = "The name of the node")
        LanguageFieldDTO<String> name,

        @JsonProperty @Schema(description = "The context path")
        String path,

        @JsonProperty @Schema(description = "The context url")
        String url) {}

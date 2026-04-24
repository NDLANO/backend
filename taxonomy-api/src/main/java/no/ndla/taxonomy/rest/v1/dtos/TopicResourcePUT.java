/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.Optional;

public class TopicResourcePUT {
    @JsonProperty
    @Schema(description = "Primary connection", example = "true")
    public Optional<Boolean> primary = Optional.empty();

    @JsonProperty
    @Schema(description = "Order in which the resource will be sorted for this topic.", example = "1")
    public Optional<Integer> rank = Optional.empty();

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId = Optional.empty();
}

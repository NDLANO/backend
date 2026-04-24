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

@Schema(requiredProperties = {"nodeId", "resourceId"})
public class NodeResourcePOST {
    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Node id", example = "urn:node:345")
    public URI nodeId;

    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource id", example = "urn:resource:345")
    public URI resourceId;

    @JsonProperty
    @Schema(description = "Primary connection", example = "true")
    public Optional<Boolean> primary = Optional.of(true);

    @JsonProperty
    @Schema(description = "Order in which resource is sorted for the node", example = "1")
    public Optional<Integer> rank = Optional.empty();

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId = Optional.empty();
}

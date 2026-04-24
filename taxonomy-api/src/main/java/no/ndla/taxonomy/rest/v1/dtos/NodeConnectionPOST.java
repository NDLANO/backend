/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.domain.NodeConnectionType;

@Schema(requiredProperties = {"parentId", "childId"})
public class NodeConnectionPOST {
    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "Parent id", example = "urn:topic:234")
    public URI parentId;

    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Child id", example = "urn:topic:234")
    public URI childId;

    @JsonProperty
    @Schema(description = "If this connection is primary.", example = "true")
    public Optional<Boolean> primary = Optional.of(true);

    @JsonProperty
    @Schema(description = "Order in which to sort the child for the parent", example = "1")
    public Optional<Integer> rank = Optional.empty();

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId = Optional.empty();

    @JsonProperty
    @Enumerated(EnumType.STRING)
    @Schema(description = "Connection type", example = "BRANCH", defaultValue = "BRANCH")
    public Optional<NodeConnectionType> connectionType = Optional.empty();
}

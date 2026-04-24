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
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.service.dtos.MetadataDTO;

@Schema(
        name = "NodeResource",
        requiredProperties = {"nodeId", "resourceId", "id", "primary", "rank", "metadata"})
public class NodeResourceDTO {

    @JsonProperty
    @Schema(description = "Node id", example = "urn:node:345")
    public URI nodeId;

    @JsonProperty
    @Schema(description = "Resource id", example = "urn:resource:345")
    public URI resourceId;

    @JsonProperty
    @Schema(description = "Node resource connection id", example = "urn:node-resource:123")
    public URI id;

    @JsonProperty
    @Schema(description = "Primary connection", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which the resource is sorted for the node", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId;

    @JsonProperty
    @Schema(description = "Metadata for entity. Read only.")
    private MetadataDTO metadata;

    public NodeResourceDTO() {}

    public NodeResourceDTO(NodeConnection nodeResource) {
        id = nodeResource.getPublicId();
        nodeResource.getParent().ifPresent(node -> nodeId = node.getPublicId());
        nodeResource.getResource().ifPresent(resource -> resourceId = resource.getPublicId());
        primary = nodeResource.isPrimary().orElse(false);
        rank = nodeResource.getRank();
        relevanceId = nodeResource.getRelevance().map(Relevance::getPublicId);
        metadata = new MetadataDTO(nodeResource.getMetadata());
    }
}

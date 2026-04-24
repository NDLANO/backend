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

@Schema(
        name = "TopicResource",
        requiredProperties = {"topicid", "resourceId", "id", "primary", "rank"})
public class TopicResourceDTO {

    @JsonProperty
    @Schema(description = "Topic id", example = "urn:topic:345")
    public URI topicid;

    @JsonProperty
    @Schema(description = "Resource id", example = "urn:resource:345")
    public URI resourceId;

    @JsonProperty
    @Schema(description = "Topic resource connection id", example = "urn:topic-has-resources:123")
    public URI id;

    @JsonProperty
    @Schema(description = "Primary connection", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which the resource is sorted for the topic", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId;

    public TopicResourceDTO(NodeConnection topicResource) {
        id = topicResource.getPublicId();
        topicResource.getParent().ifPresent(topic -> topicid = topic.getPublicId());
        topicResource.getResource().ifPresent(resource -> resourceId = resource.getPublicId());
        primary = topicResource.isPrimary().orElse(false);
        rank = topicResource.getRank();
        relevanceId = topicResource.getRelevance().map(Relevance::getPublicId);
    }

    TopicResourceDTO() {}
}

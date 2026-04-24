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
        name = "TopicSubtopic",
        requiredProperties = {"topicid", "subtopicid", "id", "primary", "rank"})
public class TopicSubtopicDTO {
    @JsonProperty
    @Schema(description = "Topic id", example = "urn:topic:234")
    public URI topicid;

    @JsonProperty
    @Schema(description = "Subtopic id", example = "urn:topic:234")
    public URI subtopicid;

    @JsonProperty
    @Schema(description = "Connection id", example = "urn:topic-has-subtopics:345")
    public URI id;

    @JsonProperty
    @Schema(description = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which subtopic is sorted for the topic", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId;

    TopicSubtopicDTO() {}

    public TopicSubtopicDTO(NodeConnection nodeConnection) {
        id = nodeConnection.getPublicId();
        nodeConnection.getParent().ifPresent(topic -> topicid = topic.getPublicId());
        nodeConnection.getChild().ifPresent(subtopic -> subtopicid = subtopic.getPublicId());
        relevanceId = nodeConnection.getRelevance().map(Relevance::getPublicId);
        primary = true;
        rank = nodeConnection.getRank();
    }
}

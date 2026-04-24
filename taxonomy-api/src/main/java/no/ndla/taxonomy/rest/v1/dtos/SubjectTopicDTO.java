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
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;

@Schema(
        name = "SubjectTopic",
        requiredProperties = {"subjectid", "topicid", "id", "primary", "rank"})
public class SubjectTopicDTO {
    @JsonProperty
    @Schema(description = "Subject id", example = "urn:subject:123")
    public URI subjectid;

    @JsonProperty
    @Schema(description = "Topic id", example = "urn:topic:345")
    public URI topicid;

    @JsonProperty
    @Schema(description = "Connection id", example = "urn:subject-has-topics:34")
    public URI id;

    @JsonProperty
    @Schema(description = "primary", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which the topic is sorted under the subject", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId;

    SubjectTopicDTO() {}

    public SubjectTopicDTO(NodeConnection nodeConnection) {
        id = nodeConnection.getPublicId();
        subjectid = nodeConnection.getParent().map(Node::getPublicId).orElse(null);
        topicid = nodeConnection.getChild().map(Node::getPublicId).orElse(null);
        primary = true;
        rank = nodeConnection.getRank();
        relevanceId = nodeConnection.getRelevance().map(Relevance::getPublicId);
    }
}

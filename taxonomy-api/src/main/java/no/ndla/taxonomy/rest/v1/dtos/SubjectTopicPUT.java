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

public class SubjectTopicPUT {
    @JsonProperty
    @Schema(
            description =
                    "If true, set this subject as the primary subject for this topic. This will replace any other primary subject for this topic. You must have one primary subject, so it is not allowed to set the currently primary subject to not be primary any more.",
            example = "true")
    public Optional<Boolean> primary = Optional.empty();

    @JsonProperty
    @Schema(description = "Order in which the topic should be sorted for the subject", example = "1")
    public Optional<Integer> rank = Optional.empty();

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId = Optional.empty();
}

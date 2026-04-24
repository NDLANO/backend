/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.service.UpdatableDto;

@Schema(requiredProperties = {"name", "contentUri"})
public class TopicPostPut implements UpdatableDto<Node> {
    @JsonProperty
    @Schema(
            description =
                    "If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.",
            example = "urn:topic:1")
    public Optional<URI> id = Optional.empty();

    @JsonProperty
    @Schema(
            description = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.",
            example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "The name of the topic",
            example = "Trigonometry")
    public String name;

    @Override
    public Optional<URI> getId() {
        return id;
    }

    @Override
    public void apply(Node topic) {
        if (getId().isPresent()) topic.setPublicId(getId().get());
        topic.setName(name);
        topic.setContentUri(contentUri);
    }
}

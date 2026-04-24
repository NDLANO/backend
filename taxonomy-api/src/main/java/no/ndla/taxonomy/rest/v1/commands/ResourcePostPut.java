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

@Schema(requiredProperties = {"contentUri", "name"})
public class ResourcePostPut implements UpdatableDto<Node> {
    @JsonProperty
    @Schema(
            description =
                    "If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.",
            example = "urn:resource:2")
    public Optional<URI> id = Optional.empty();

    @JsonProperty
    @Schema(
            description =
                    "The ID of this resource in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier "
                            + "for the system, and <id> is the id of this content in that system.",
            example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "The name of the resource",
            example = "Introduction to integration")
    public String name;

    @Override
    public Optional<URI> getId() {
        return id;
    }

    @Override
    public void apply(Node entity) {
        if (getId().isPresent()) entity.setPublicId(getId().get());
        entity.setName(name);
        entity.setContentUri(contentUri);
    }
}

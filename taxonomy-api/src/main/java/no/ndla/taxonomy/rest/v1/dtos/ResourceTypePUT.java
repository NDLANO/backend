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
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.service.UpdatableDto;

@Schema(requiredProperties = {"name", "id", "parentId"})
public class ResourceTypePUT implements UpdatableDto<ResourceType> {
    @JsonProperty
    @Schema(description = "If specified, the new resource type will be a child of the mentioned resource type.")
    public URI parentId;

    @JsonProperty
    @Schema(
            description =
                    "If specified, set the id to this value. Must start with urn:resourcetype: and be a valid URI. If omitted, an id will be assigned automatically.",
            example = "urn:resourcetype:1")
    public URI id;

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "The name of the resource type",
            example = "Lecture")
    public String name;

    @JsonProperty
    @Schema(description = "Order in which the resource type should be sorted among its siblings")
    public int order = -1;

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public void apply(ResourceType entity) {
        entity.setName(name);
        if (order > -1) {
            entity.setOrder(order);
        }
    }
}

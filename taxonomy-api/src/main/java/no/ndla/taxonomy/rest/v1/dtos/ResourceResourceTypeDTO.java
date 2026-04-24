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
import no.ndla.taxonomy.domain.ResourceResourceType;

@Schema(
        name = "ResourceResourceType",
        requiredProperties = {"id", "resourceId", "resourceTypeId"})
public class ResourceResourceTypeDTO {
    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource type id", example = "urn:resource:123")
    public URI resourceId;

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Resource type id",
            example = "urn:resourcetype:234")
    public URI resourceTypeId;

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Resource to resource type connection id",
            example = "urn:resource-has-resourcetypes:12")
    public URI id;

    public ResourceResourceTypeDTO() {}

    public ResourceResourceTypeDTO(ResourceResourceType resourceResourceType) {
        id = resourceResourceType.getPublicId();
        resourceId = resourceResourceType.getNode().getPublicId();
        resourceTypeId = resourceResourceType.getResourceType().getPublicId();
    }
}

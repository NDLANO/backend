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

@Schema(requiredProperties = {"resourceId", "resourceTypeId"})
public class ResourceResourceTypePOST {
    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource id", example = "urn:resource:123")
    public URI resourceId;

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Resource type id",
            example = "urn:resourcetype:234")
    public URI resourceTypeId;
}

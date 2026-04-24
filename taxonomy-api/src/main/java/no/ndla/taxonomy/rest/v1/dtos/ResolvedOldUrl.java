/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"path"})
public class ResolvedOldUrl {
    @JsonProperty
    @Schema(description = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
    public String path;
}

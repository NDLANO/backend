/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Schema(name = "MetadataPUT")
public class MetadataPUT {
    @JsonProperty
    @Schema(description = "Set of grep codes, Only updated if present")
    public Optional<Set<String>> grepCodes = Optional.empty();

    @JsonProperty
    @Schema(description = "Visibility of the node, Only updated if present")
    public Optional<Boolean> visible = Optional.empty();

    @JsonProperty
    @Schema(description = "Custom fields, Only updated if present")
    public Optional<Map<String, String>> customFields = Optional.empty();

    public MetadataPUT() {}
}

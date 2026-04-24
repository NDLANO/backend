/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.JsonGrepCode;
import no.ndla.taxonomy.domain.Metadata;

@Schema(
        name = "Metadata",
        requiredProperties = {"grepCodes", "visible", "customFields"})
public class MetadataDTO {
    @Schema
    public Set<String> grepCodes;

    @Schema
    public Boolean visible;

    @Schema
    public Map<String, String> customFields;

    public MetadataDTO() {}

    public MetadataDTO(Metadata metadata) {
        this.visible = metadata.isVisible();
        this.grepCodes =
                metadata.getGrepCodes().stream().map(JsonGrepCode::code).collect(Collectors.toSet());
        this.customFields = metadata.getCustomFields();
    }

    public Set<String> getGrepCodes() {
        return grepCodes;
    }

    public void setGrepCodes(Set<String> grepCodes) {
        this.grepCodes = grepCodes;
    }

    public Boolean isVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }
}

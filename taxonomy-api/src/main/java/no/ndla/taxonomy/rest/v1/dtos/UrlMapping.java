/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

@Schema(requiredProperties = {"url", "nodeId", "subjectId"})
public class UrlMapping {
    @Schema(description = "URL for resource in old system", example = "ndla.no/nb/node/183926?fag=127013")
    @JsonProperty
    public String url;

    @Schema(description = "Node URN for resource in new system", example = "urn:topic:1:183926")
    @JsonProperty
    public String nodeId;

    @Schema(description = "Subject URN for resource in new system (optional)", example = "urn:subject:5")
    @JsonProperty
    public String subjectId;

    @JsonCreator
    public UrlMapping() {}

    public UrlMapping(String url, URI nodeId, URI subjectId) {
        this.url = url;
        this.nodeId = nodeId.toString();
        this.subjectId = subjectId.toString();
    }
}

/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;

@Schema(requiredProperties = {"id", "contentUri", "name", "parents", "path", "url", "exactMatch"})
public class ResolvedUrl {
    @Schema(description = "ID of the element referred to by the given path", example = "urn:resource:1")
    private URI id;

    @JsonProperty
    @Schema(
            description =
                    "The ID of this element in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier "
                            + "for the system, and <id> is the id of this content in that system.",
            example = "urn:article:1")
    private URI contentUri;

    @JsonProperty
    @Schema(
            description =
                    "Element name. For performance reasons, this "
                            + "name is for informational purposes only. To get a translated name, please fetch the resolved resource using its rest resource.",
            example = "Basic physics")
    private String name;

    @JsonProperty
    @Schema(
            description =
                    "Parent elements of the resolved element. The first element is the parent, the second is the grandparent, etc.")
    private List<URI> parents;

    @JsonProperty
    @Schema(description = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
    private String path;

    @JsonProperty
    @Schema(description = "Pretty url resource", example = "'/r/subject-name/resource-name/hash'")
    private String url;

    @JsonProperty
    @Schema(
            description =
                    "Is this an exact match for the provided path? False if this is another path to the same resource.")
    private boolean exactMatch;

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<URI> getParents() {
        return parents;
    }

    public void setParents(List<URI> parents) {
        this.parents = parents;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }
}
